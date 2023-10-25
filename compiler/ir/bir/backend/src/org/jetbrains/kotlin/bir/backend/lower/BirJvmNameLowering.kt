/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.bir.BirElementDynamicPropertyKey
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.backend.jvm.JvmBirBackendContext
import org.jetbrains.kotlin.bir.declarations.BirFunction
import org.jetbrains.kotlin.bir.declarations.BirModuleFragment
import org.jetbrains.kotlin.bir.expressions.BirConst
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.set
import org.jetbrains.kotlin.bir.util.constructedClass
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.types.impl.IrErrorClassImpl.origin
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

context(JvmBirBackendContext)
class BirJvmNameLowering : BirLoweringPhase() {
    private val JvmNameAnnotation = birBuiltIns.findClass(DescriptorUtils.JVM_NAME)!!

    private val jvmNameKey = acquireProperty(JvmName)
    private val jvmNameAnnotations = registerIndexKey<BirConstructorCall>(false) {
        it.constructedClass == JvmNameAnnotation
    }

    override fun invoke(module: BirModuleFragment) {
        compiledBir.getElementsWithIndex(jvmNameAnnotations).forEach { annotation ->
            val function = annotation.parent as? BirFunction ?: return@forEach

            val const = annotation.valueArguments[0] as? BirConst<*> ?: return@forEach
            val value = const.value as? String ?: return@forEach
            val name = when (origin) {
                IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER -> "$value\$default"
                JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE,
                JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE,
                -> "$value$FOR_INLINE_SUFFIX"
                else -> value
            }

            function[jvmNameKey] = Name.identifier(name)
        }
    }

    companion object {
        val JvmName = BirElementDynamicPropertyKey<BirFunction, Name>()
    }
}