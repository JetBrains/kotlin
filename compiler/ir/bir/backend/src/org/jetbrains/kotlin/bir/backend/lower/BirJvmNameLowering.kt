/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.lower

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.bir.GlobalBirDynamicProperty
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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

context(JvmBirBackendContext)
class BirJvmNameLowering : BirLoweringPhase() {
    private val JvmNameAnnotation by lz { birBuiltIns.findClass(DescriptorUtils.JVM_NAME) }

    private val jvmNameAnnotations = registerIndexKey(BirConstructorCall, false) {
        it.constructedClass == JvmNameAnnotation
    }

    override fun lower(module: BirModuleFragment) {
        getAllElementsWithIndex(jvmNameAnnotations).forEach { annotation ->
            val function = annotation.parent as? BirFunction ?: return@forEach

            val const = annotation.valueArguments[0] as? BirConst<*> ?: return@forEach
            val value = const.value as? String ?: return@forEach
            val name = when (function.origin) {
                IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER -> "$value\$default"
                JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE,
                JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE,
                -> "$value$FOR_INLINE_SUFFIX"
                else -> value
            }

            function[JvmName] = Name.identifier(name)
        }
    }

    companion object {
        val JvmName = GlobalBirDynamicProperty<_, Name>(BirFunction)
    }
}