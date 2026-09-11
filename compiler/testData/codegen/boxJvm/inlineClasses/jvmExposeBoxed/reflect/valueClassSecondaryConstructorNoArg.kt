// WITH_REFLECT
// TARGET_BACKEND: JVM

// KT-89068: the no-arg constructor is generated at most once - the primary constructor claims it first.

@file:OptIn(ExperimentalStdlibApi::class)

package test

@JvmExposeBoxed
@JvmInline
value class PrimaryClaims(val a: UInt = 1u) {
    constructor(param1: UInt = 2u, param2: String = "") : this(param1)
}

@JvmExposeBoxed
@JvmInline
value class SecondaryClaims(val a: UInt) {
    constructor(param1: UInt = 2u, param2: String = "") : this(param1)
}

fun box(): String {
    val claimedByPrimary = PrimaryClaims::class.java.declaredConstructors.filter { it.parameterTypes.size == 0 }
    if (claimedByPrimary.size != 1) return "FAIL 1: ${claimedByPrimary.map { it.toString() }}"
    val fromPrimary = claimedByPrimary.single().newInstance() as PrimaryClaims
    if (fromPrimary.a != 1u) return "FAIL 2: ${fromPrimary.a}"

    val claimedBySecondary = SecondaryClaims::class.java.declaredConstructors.filter { it.parameterTypes.size == 0 }
    if (claimedBySecondary.size != 1) return "FAIL 3: ${claimedBySecondary.map { it.toString() }}"
    val fromSecondary = claimedBySecondary.single().newInstance() as SecondaryClaims
    if (fromSecondary.a != 2u) return "FAIL 4: ${fromSecondary.a}"

    return "OK"
}
