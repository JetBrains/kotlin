/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.KmValueParameter
import kotlin.metadata.declaresDefaultValue
import kotlin.reflect.KParameter
import kotlin.reflect.KType

internal class KotlinKParameter(
    override val callable: KotlinKCallable<*>,
    internal val kmParameter: KmValueParameter,
    override val index: Int,
    override val kind: KParameter.Kind,
    typeParameterTable: TypeParameterTable,
) : ReflectKParameter() {
    override val name: String? =
        kmParameter.name.takeUnless { it.startsWith("<") }

    override val type: KType by lazy(PUBLICATION) {
        callable.substituteType(kmParameter.type.toKType(callable.container.jClass.safeClassLoader, typeParameterTable) {
            callable.caller.parameterTypes[index]
        })
    }

    override val isOptional: Boolean by lazy(PUBLICATION) {
        if (kmParameter.declaresDefaultValue) return@lazy true
        val overridden = (callable as? KotlinKFunction)?.overridden ?: return@lazy false
        // If this parameter's callable has bound receiver(s), its index needs to be adjusted because functions returned by
        // `ReflectKFunction.overridden` are always unbound.
        // TODO(KT-86452): make sure to update this code once bound references with context receivers are supported.
        val unboundIndex = if (callable.isBound) index + 1 else index
        overridden.any { it.parameters[unboundIndex].isOptional }
    }

    override val declaresDefaultValue: Boolean
        get() = kmParameter.declaresDefaultValue

    override val isVararg: Boolean
        get() = kmParameter.varargElementType != null
}
