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
        require(declaration !in numeration)
        numeration[declaration] = currentNumber
        currentNumber++
    }

    fun add(packageFragment: IrPackageFragment) {
        packageFragment.declarations.forEach {
            add(it)
            // Number member fields
            it.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitField(declaration: IrField) {
                    if (declaration.parent is IrClass) {
                        add(declaration)
                    }
                    super.visitField(declaration)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {

                    // Add interfaces default impls
                    val parentClass = declaration.parent as? IrClass
                    val isInterfaceImpl = parentClass?.isInterface ?: false
                    if (isInterfaceImpl && declaration.body != null) {
                        add(declaration)
                    }
                    super.visitSimpleFunction(declaration)
                }
            })
        }
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
        val className = getNameForStaticDeclaration(field.parentAsClass)
        val fieldName = sanitizeName(exportId(field))
        return JsName(className.toString() + "_f_" + fieldName)
    }

    override fun getNameForStaticDeclaration(declaration: IrDeclarationWithName): JsName {
        staticNames.names[declaration]?.let { return JsName(it) }

        when {
            declaration.isEffectivelyExternal() && (declaration.getJsModule() == null || declaration.isJsNonModule()) -> {
                val jsQualifier = (declaration.parent as? IrFile)?.getJsQualifier()
                var name = declaration.getJsNameOrKotlinName().identifier
                if (jsQualifier != null)
                    name = "$jsQualifier.$name"

                staticNames.declareStableName(declaration, name)
            }

            else -> {
                if (declaration.isEffectivelyExternal() && declaration.getJsModule() != null) {
                    error("PER_FILE_ERROR: JsModule")
                }

                staticNames.declareFreshName(declaration, declaration.name.asString())
                val unitReference = unit.getDeclarationUnitReference(declaration)
                if (unitReference is IrToJs.DeclarationUnitReference.OtherUnit) {
                    val import = internalImports.getOrPut(unitReference.importId) {
                        JsImport(unitReference.importId)
                    }
                    import.elements += JsImport.Element(exportId(declaration), staticNames.names[declaration]!!)
                }
            }
        }

        return JsName(staticNames.names[declaration]!!)
    }
}

private fun stableNameForExternalDeclaration(declaration: IrDeclaration): String? {
    if (declaration !is IrDeclarationWithName ||
        !declaration.hasStaticDispatch() ||
        !declaration.isEffectivelyExternal() ||
        declaration.isPropertyAccessor ||
        declaration.isPropertyField ||
        declaration is IrConstructor
    ) {
        return null
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

