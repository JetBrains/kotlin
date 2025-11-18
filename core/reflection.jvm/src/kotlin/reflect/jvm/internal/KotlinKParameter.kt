/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.KmValueParameter
import kotlin.metadata.declaresDefaultValue
import kotlin.reflect.KParameter
import kotlin.reflect.KType

internal class KotlinKParameter(
    override val callable: KotlinKCallable<*>,
    private val kmParameter: KmValueParameter,
    override val index: Int,
    override val kind: KParameter.Kind,
    typeParameterTable: TypeParameterTable,
) : ReflectKParameter() {
    override val name: String? =
        kmParameter.name.takeUnless { it.startsWith("<") }

    override val type: KType by lazy(PUBLICATION) {
        kmParameter.type.toKType(callable.container.jClass.classLoader, typeParameterTable) {
            require(callable.container is KPackageImpl || callable.isConstructor) {
                // For class callables, we'll also need to tweak instance receiver parameter type (see `DescriptorKParameter`).
                "Only constructors and top-level callables are supported for now: $callable"
            }
            callable.caller.parameterTypes[index]
        }
    }

    override val isOptional: Boolean
        get() {
            require(callable is KotlinKProperty<*> || callable.container is KPackageImpl || callable.isConstructor) {
                // For class functions, we'll also need to check the flag for parameters from inherited functions.
                "Only constructors and top-level callables are supported for now: $callable"
            }
            return kmParameter.declaresDefaultValue
        }

    override val declaresDefaultValue: Boolean
        get() = kmParameter.declaresDefaultValue

    override val isVararg: Boolean
        get() = kmParameter.varargElementType != null
}
