/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.reflect.KParameter
import kotlin.reflect.KType

internal class JavaKParameter(
    override val callable: JavaKCallable<*>,
    override val name: String?,
    override val type: KType,
    override val index: Int,
    override val kind: KParameter.Kind,
    override val isVararg: Boolean,
) : ReflectKParameter() {
    override val isOptional: Boolean get() = false
    override val declaresDefaultValue: Boolean get() = false
}
