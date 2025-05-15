/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.InventNamesForLocalClasses
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.localClassType
import org.jetbrains.kotlin.codegen.sanitizeNameIfNeeded
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type

@PhaseDescription(
    name = "InventNamesForLocalClasses",
    // MainMethodGeneration introduces lambdas, needing names for their local classes.
    prerequisite = [MainMethodGenerationLowering::class],
)
internal class JvmInventNamesForLocalClasses(private val context: JvmBackendContext) : InventNamesForLocalClasses() {
    private val LAMBDA_NAME_SUFFIX = "$0"

    override fun computeTopLevelClassName(clazz: IrClass): String {
        val file = clazz.parent as? IrFile
            ?: throw AssertionError("Top-level class expected: ${clazz.render()}")
        val classFqn =
            if (clazz.origin == IrDeclarationOrigin.FILE_CLASS ||
                clazz.origin == IrDeclarationOrigin.SYNTHETIC_FILE_CLASS
            ) {
                file.getFileClassInfo().fileClassFqName
            } else {
                file.packageFqName.child(clazz.name)
            }
        return JvmClassName.byFqNameWithoutInnerClasses(classFqn).internalName
    }

    override fun sanitizeNameIfNeeded(name: String): String {
        return sanitizeNameIfNeeded(name, context.config.languageVersionSettings)
    }

    override fun putLocalClassName(declaration: IrElement, localClassName: String) {
        // We can visit the same class twice: before IR inlining and after. The name that was before is more preferable.
        if (declaration.localClassType != null) return
        val newLocalClassName = when {
            declaration is IrFunctionReference && declaration.isLambda -> {
                "$localClassName${generateLambdaSuperClassSuffix(declaration)}"
            }
            else -> localClassName
        }
        declaration.localClassType = Type.getObjectType(newLocalClassName)
    }

    private fun generateLambdaSuperClassSuffix(declaration: IrFunctionReference): String {
        val parameterTypes = (declaration.type as? IrSimpleType)?.arguments?.map {
            when (it) {
                is IrTypeProjection -> it.type
                is IrStarProjection -> context.irBuiltIns.anyNType
            }
        } ?: return ""
        val argumentsSize = parameterTypes.size - 1
        val functionSuperClass = when {
            declaration.isSuspend -> context.irBuiltIns.suspendFunctionN(argumentsSize).symbol
            else -> context.irBuiltIns.functionN(argumentsSize).symbol
        }
        val superTypeSuffix = functionSuperClass.typeWith(parameterTypes).classFqName!!.asString().replace('.', '_')
        return "\$$superTypeSuffix$LAMBDA_NAME_SUFFIX"
    }
}
