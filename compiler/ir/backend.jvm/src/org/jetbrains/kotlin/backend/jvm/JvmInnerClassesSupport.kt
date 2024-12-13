/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

private var IrClass.innerClassOuterThisField: IrField? by irAttribute(followAttributeOwner = false)
private var IrConstructor.innerClassConstructorWithOuterThisParameter: IrConstructor? by irAttribute(followAttributeOwner = false)
private var IrClass.innerClassOriginalPrimaryConstructor: IrConstructor? by irAttribute(followAttributeOwner = false)

class JvmInnerClassesSupport(private val irFactory: IrFactory) : InnerClassesSupport {
    override fun getOuterThisField(innerClass: IrClass): IrField =
        innerClass::innerClassOuterThisField.getOrSetIfNull {
            assert(innerClass.isInner) { "Class is not inner: ${innerClass.dump()}" }
            irFactory.buildField {
                name = Name.identifier("this$0")
                type = innerClass.parentAsClass.defaultType
                origin = IrDeclarationOrigin.FIELD_FOR_OUTER_THIS
                visibility = JavaDescriptorVisibilities.PACKAGE_VISIBILITY
                isFinal = true
            }.apply {
                parent = innerClass
            }
        }

    override fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: IrConstructor): IrConstructor {
        val innerClass = innerClassConstructor.parent as IrClass
        assert(innerClass.isInner) { "Class is not inner: ${(innerClassConstructor.parent as IrClass).dump()}" }

        return innerClassConstructor::innerClassConstructorWithOuterThisParameter.getOrSetIfNull {
            createInnerClassConstructorWithOuterThisParameter(innerClassConstructor)
        }.also {
            if (innerClassConstructor.isPrimary) {
                innerClass.innerClassOriginalPrimaryConstructor = innerClassConstructor
            }
        }
    }

    override fun getInnerClassOriginalPrimaryConstructorOrNull(innerClass: IrClass): IrConstructor? {
        assert(innerClass.isInner) { "Class is not inner: $innerClass" }
        return innerClass.innerClassOriginalPrimaryConstructor
    }

    private fun createInnerClassConstructorWithOuterThisParameter(oldConstructor: IrConstructor): IrConstructor =
        irFactory.buildConstructor {
            updateFrom(oldConstructor)
            returnType = oldConstructor.returnType
        }.apply {
            parent = oldConstructor.parent
            returnType = oldConstructor.returnType
            copyAnnotationsFrom(oldConstructor)
            copyTypeParametersFrom(oldConstructor)

            val outerThisValueParameter = buildValueParameter(this) {
                kind = IrParameterKind.Regular
                origin = JvmLoweredDeclarationOrigin.FIELD_FOR_OUTER_THIS
                name = Name.identifier(AsmUtil.CAPTURED_THIS_FIELD)
                type = oldConstructor.parentAsClass.parentAsClass.defaultType
            }
            parameters = listOf(outerThisValueParameter) + oldConstructor.nonDispatchParameters.map { it.copyTo(this) }
            metadata = oldConstructor.metadata
        }
}
