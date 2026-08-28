// Version overloads next to a hand-written `@Deprecated` declaration.
//
// Besides checking that the wrappers are callable on every backend, this covers KT-87965 on the KLIB backends:
// `VersionOverloadsLowering` adds `@Deprecated` version-overload wrappers to the KLIB ABI before serialization, so
// `CHECK_SAME_ABI_AFTER_INLINING` must tolerate them (see `AbiDeclarationOrigin`) while still
// verifying the declarations around them, including the hand-written `@Deprecated` one.
@file:OptIn(ExperimentalVersionOverloading::class)

fun topLevelFun(a: Int, @IntroducedAt("1") b: String = "b", @IntroducedAt("2") c: String = "c") = a.toString() + b + c

@Deprecated("Not a version overload wrapper", level = DeprecationLevel.WARNING)
fun deprecatedByHand(a: Int = 1) = a

class WithVersionedConstructor(val a: Int, @IntroducedAt("1") val b: String = "b") {
    fun memberFun(a: Int, @IntroducedAt("1") b: String = "b") = a.toString() + b
}

data class VersionedDataClass(val a: Int, @IntroducedAt("1") val b: String = "b")

@Suppress("DEPRECATION")
fun box(): String {
    if (topLevelFun(1) != "1bc") return "fail1: ${topLevelFun(1)}"
    if (deprecatedByHand() != 1) return "fail2"

    val withVersionedConstructor = WithVersionedConstructor(2)
    if (withVersionedConstructor.b != "b") return "fail3"
    if (withVersionedConstructor.memberFun(3) != "3b") return "fail4"

    val dataClass = VersionedDataClass(4)
    if (dataClass.copy(a = 5).toString() != "VersionedDataClass(a=5, b=b)") return "fail5: ${dataClass.copy(a = 5)}"

    return "OK"
}
