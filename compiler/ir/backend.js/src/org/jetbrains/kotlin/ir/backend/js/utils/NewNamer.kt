/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.codegen.IrToJs
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.js.backend.ast.JsImport
import org.jetbrains.kotlin.js.backend.ast.JsName

class StaticDeclarationNumerator {
    var currentNumber = 0
    val numeration = mutableMapOf<IrDeclaration, Int>()

    fun add(moduleFragment: IrModuleFragment) {
        moduleFragment.files.forEach { add(it) }
    }

    fun add(declaration: IrDeclaration) {
        // TODO: We should not visit declarations multiple times.
        //       Investigate enum tests in dce-driven mode.
        if (declaration !in numeration) {
            numeration[declaration] = currentNumber
            currentNumber++
        }
    }

    fun add(packageFragment: IrPackageFragment) {
        packageFragment.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitDeclaration(declaration: IrDeclarationBase) {
                if (declaration !is IrVariable) {
                    add(declaration)
                }
                super.visitDeclaration(declaration)
            }
        })
    }
}

class NewStableStaticNamesCollectorVisitor(val needToCollectReferences: Boolean) : IrElementVisitorVoid {
    val collectedStableNames = mutableSetOf<String>()

    init {
        collectedStableNames.addAll(RESERVED_IDENTIFIERS)
        collectedStableNames.add(Namer.IMPLICIT_RECEIVER_NAME)
    }

    private fun IrDeclaration.collectStableName() {
        collectedStableNames += stableNameForExternalDeclaration(this) ?: return
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        super.visitDeclaration(declaration)
        declaration.collectStableName()
    }

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        super.visitDeclarationReference(expression)
        if (needToCollectReferences) {
            val declaration = expression.symbol.owner as? IrDeclaration
            declaration?.collectStableName()
        }
    }
}

class NewNamerImpl(
    val context: JsIrBackendContext,
    val unit: IrToJs.CodegenUnit,
    val exportId: (IrDeclarationWithName) -> String,
    val stableNames: Set<String>,
) : IrNamerBase() {
    val staticNames = NameTable<IrDeclaration>(
        reserved = stableNames.toMutableSet()
    )
    val internalImports = mutableMapOf<String, JsImport>()

    override fun getNameForMemberFunction(function: IrSimpleFunction): JsName {
        require(function.dispatchReceiverParameter != null)
        val name = jsFunctionSignature(function, context)
        return name.toJsName()
    }

    override fun getNameForMemberField(field: IrField): JsName {
        val fieldName = sanitizeName(
            try {
                exportId(field)
            } catch (e: IllegalStateException) {
                // TODO: Fix DCE with inline classes and remove this hack
                field.name.asString() + "_LIKELY_ELIMINATED_BY_DCE"
            }
        )
        // TODO: Webpack not minimize member names, it is long name, which is not minimized, so it affects final JS bundle size
        // Use shorter names
        return JsName("f_$fieldName", false)
    }

    override fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): JsName {
        staticNames.names[declaration]?.let { return JsName(it, false) }

        fun registerImport(moduleId: String, importedName: String) {
            val fullModuleId = if (moduleId.startsWith(".")) {
                unit.pathToKotlinModulesRoot + moduleId
            } else {
                // TODO: Do we cover this path in tests?
                moduleId
            }

            val import = internalImports.getOrPut(fullModuleId) {
                JsImport(fullModuleId)
            }
            import.elements += JsImport.Element(importedName, staticNames.names[declaration]!!)
        }

        if (declaration.isEffectivelyExternal()) {
            val jsModule: String? = declaration.getJsModule()
            val maybeParentFile: IrFile? = declaration.parent as? IrFile
            val fileJsModule: String? = maybeParentFile?.getJsModule()
            val jsQualifier: String? = maybeParentFile?.getJsQualifier()

            when {
                jsModule != null -> {
                    // TODO: Support jsQualifier
                    staticNames.declareFreshName(declaration, declaration.name.asString())
                    registerImport(jsModule, "default")
                }

                fileJsModule != null -> {
                    // TODO: Support jsQualifier
                    staticNames.declareFreshName(declaration, declaration.name.asString())
                    registerImport(fileJsModule, declaration.getJsNameOrKotlinName().identifier)
                }

                else -> {
                    var name = declaration.getJsNameOrKotlinName().identifier
                    if (jsQualifier != null)
                        name = "$jsQualifier.$name"

                    staticNames.declareStableName(declaration, name)
                }
            }


        } else {  // Non-external declaration
            val name = declaration.nameIfPropertyAccessor() ?: declaration.name.asString()
            staticNames.declareFreshName(declaration, name)
            val unitReference = unit.referenceCodegenUnitOfDeclaration(declaration)
            if (unitReference is IrToJs.OtherUnitReference) {
                registerImport(unitReference.importPath, exportId(declaration))
            }
        }

        return JsName(staticNames.names[declaration]!!, false)
    }
}

// TODO: Cache?
private fun stableNameForExternalDeclaration(declaration: IrDeclaration): String? {
    if (declaration !is IrDeclarationWithName ||
        !declaration.hasStaticDispatch() ||
        !declaration.isEffectivelyExternal() ||
        declaration.isPropertyAccessor ||
        declaration.isPropertyField
    ) {
        return null
    }

    if (declaration is IrConstructor) {
        return stableNameForExternalDeclaration(declaration.parentAsClass)
    }

    val importedFromModuleOnly =
        declaration.getJsModule() != null && !declaration.isJsNonModule()

    val jsName = declaration.getJsName()

    val jsQualifier = declaration.fileOrNull?.getJsQualifier()

    return when {
        importedFromModuleOnly ->
            null

        jsQualifier != null ->
            jsQualifier.split('1')[0]

        jsName != null ->
            jsName

        else ->
            declaration.name.identifier
    }
}

