/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.inline.addChild
import org.jetbrains.kotlin.ir.backend.js.utils.getJsModule
import org.jetbrains.kotlin.ir.backend.js.utils.getJsQualifier
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

fun moveBodilessDeclarationsToSeparatePlace(context: JsIrBackendContext, module: IrModuleFragment) {

    val builtInClasses = listOf(
        "String",
        "Nothing",
        "Array",
        "Any",
        "ByteArray",
        "CharArray",
        "ShortArray",
        "IntArray",
        "LongArray",
        "FloatArray",
        "DoubleArray",
        "BooleanArray",
        "Boolean",
        "Byte",
        "Short",
        "Int",
        "Float",
        "Double",
        "Function"
    ).map { Name.identifier(it) }.toSet()

    fun isBuiltInClass(declaration: IrDeclaration): Boolean =
        declaration is IrClass && declaration.name in builtInClasses

    val packageFragmentMap = mutableMapOf<FqName, IrExternalPackageFragment>()

    fun collectExternalClasses(container: IrDeclarationContainer, includeCurrentLevel: Boolean): List<IrClass> {
        val externalClasses =
            container.declarations.filterIsInstance<IrClass>().filter { it.isEffectivelyExternal() }

        val nestedExternalClasses =
            externalClasses.flatMap { collectExternalClasses(it, true) }

        return if (includeCurrentLevel)
            externalClasses + nestedExternalClasses
        else
            nestedExternalClasses
    }

    fun lowerFile(irFile: IrFile): IrFile? {
        context.externalNestedClasses += collectExternalClasses(irFile, includeCurrentLevel = false)

        if (irFile.getJsModule() != null || irFile.getJsQualifier() != null) {
            context.packageLevelJsModules.add(irFile)
            return null
        }

        val it = irFile.declarations.iterator()

        val packageFragment = packageFragmentMap.getOrPut(irFile.fqName) {
            IrExternalPackageFragmentImpl(object : IrExternalPackageFragmentSymbol {
                override val descriptor: PackageFragmentDescriptor
                    get() = error("Operation is unsupported")

                private var _owner: IrExternalPackageFragment? = null
                override val owner get() = _owner!!

                override val isBound get() = _owner != null

                override fun bind(owner: IrExternalPackageFragment) {
                    _owner = owner
                }
            }, irFile.fqName)
        }

        while (it.hasNext()) {
            val d = it.next()

            if (d.isEffectivelyExternal() || isBuiltInClass(d)) {

                if (d.getJsModule() != null)
                    context.declarationLevelJsModules.add(d)

                it.remove()
                packageFragment.addChild(d)
            }
        }
        return irFile
    }

    module.files.transformFlat { irFile ->
        listOfNotNull(lowerFile(irFile))
    }
}
