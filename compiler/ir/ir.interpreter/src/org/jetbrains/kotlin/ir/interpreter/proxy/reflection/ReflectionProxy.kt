/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.state.reflection.*
import kotlin.reflect.KVisibility

internal interface ReflectionProxy : Proxy {
    fun DescriptorVisibility.toKVisibility(): KVisibility? {
        return when (this) {
            DescriptorVisibilities.PUBLIC -> KVisibility.PUBLIC
            DescriptorVisibilities.PROTECTED -> KVisibility.PROTECTED
            DescriptorVisibilities.INTERNAL -> KVisibility.INTERNAL
            DescriptorVisibilities.PRIVATE -> KVisibility.PRIVATE
            else -> null
        }
    }

    companion object {
        internal fun ReflectionState.asProxy(callInterceptor: CallInterceptor): ReflectionProxy {
            return when (this) {
                is KPropertyState -> when {
                    this.isKMutableProperty0() -> KMutableProperty0Proxy(this, callInterceptor)
                    this.isKProperty0() -> KProperty0Proxy(this, callInterceptor)
                    this.isKMutableProperty1() -> KMutableProperty1Proxy(this, callInterceptor)
                    this.isKProperty1() -> KProperty1Proxy(this, callInterceptor)
                    this.isKMutableProperty2() -> KMutableProperty2Proxy(this, callInterceptor)
                    this.isKProperty2() -> KProperty2Proxy(this, callInterceptor)
                    else -> TODO()
                }
                is KFunctionState -> KFunctionProxy(this, callInterceptor)
                is KClassState -> KClassProxy(this, callInterceptor)
                is KTypeState -> KTypeProxy(this, callInterceptor)
                is KTypeParameterState -> KTypeParameterProxy(this, callInterceptor)
                is KParameterState -> KParameterProxy(this, callInterceptor)
                else -> TODO("not supported reference state")
            }
        }
    }
}
