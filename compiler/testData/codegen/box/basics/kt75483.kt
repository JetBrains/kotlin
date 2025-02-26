/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

interface FeatureFlag<OptionType : Any> {
    val default: OptionType
}

abstract class BooleanFeatureFlag(
    override val default: Boolean,
) : FeatureFlag<Boolean>

fun <T : Any> currentValueOf(flag: FeatureFlag<T>): String {
    if (flag !is BooleanFeatureFlag) return "Fail 1"
    val default: Boolean = flag.default
    return if (default) "OK" else "Fail 2"
}

class BooleanFeatureFlagImpl: BooleanFeatureFlag(true)

fun box(): String {
    return currentValueOf<Boolean>(BooleanFeatureFlagImpl())
}
