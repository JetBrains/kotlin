/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.filterOutAnnotations
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver

internal val objectClassPhase = makeIrFilePhase(
    ::ObjectClassLowering,
    name = "ObjectClass",
    description = "Handle object classes"
)

private class ObjectClassLowering(val context: JvmBackendContext) : ClassLoweringPass {
    private val pendingTransformations = mutableListOf<Function0<Unit>>()

    override fun lower(irFile: IrFile) {
        super.lower(irFile)
        for (transformation in pendingTransformations) {
            transformation.invoke()
        }
    }

    override fun lower(irClass: IrClass) {
        if (!irClass.isObject) return

        val publicInstanceField = context.cachedDeclarations.getFieldForObjectInstance(irClass)
        val privateInstanceField = context.cachedDeclarations.getPrivateFieldForObjectInstance(irClass)

        val constructor = irClass.constructors.find { it.isPrimary }
            ?: throw AssertionError("Object should have a primary constructor: ${irClass.name}")

        if (privateInstanceField != publicInstanceField) {
            with(context.createIrBuilder(privateInstanceField.symbol)) {
                privateInstanceField.initializer = irExprBody(irCall(constructor.symbol))
            }
            with(context.createIrBuilder(publicInstanceField.symbol)) {
                publicInstanceField.initializer = irExprBody(irGetField(null, privateInstanceField))
            }
            pendingTransformations.add {
                (privateInstanceField.parent as IrDeclarationContainer).declarations.add(0, privateInstanceField)
            }
        } else {
            with(context.createIrBuilder(publicInstanceField.symbol)) {
                publicInstanceField.initializer = irExprBody(irCall(constructor.symbol))
            }
        }

        // Mark object instance field as deprecated if the object visibility is private or protected,
        // and ProperVisibilityForCompanionObjectInstanceField language feature is not enabled.
        if (!context.state.languageVersionSettings.supportsFeature(LanguageFeature.ProperVisibilityForCompanionObjectInstanceField) &&
            (irClass.visibility == DescriptorVisibilities.PRIVATE || irClass.visibility == DescriptorVisibilities.PROTECTED)
        ) {
            context.createJvmIrBuilder(irClass.symbol).run {
                publicInstanceField.annotations =
                    filterOutAnnotations(DeprecationResolver.JAVA_DEPRECATED, publicInstanceField.annotations) +
                            irCall(irSymbols.javaLangDeprecatedConstructorWithDeprecatedFlag)
            }
        }

        pendingTransformations.add {
            (publicInstanceField.parent as IrDeclarationContainer).declarations.add(0, publicInstanceField)
        }
    }
}
