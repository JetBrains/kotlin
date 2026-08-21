// Version overloads next to a hand-written `@Deprecated` declaration.
//
// Besides checking that the wrappers are callable on every backend, this covers KT-87965 on the KLIB backends:
// `VersionOverloadsLowering` adds `@Deprecated` version-overload wrappers to the KLIB ABI before serialization, so
// `CHECK_SAME_ABI_AFTER_INLINING` must tolerate them (see `AbiDeclarationOrigin`) while still
// verifying the declarations around them, including the hand-written `@Deprecated` one.
// Android D8 rejects the generated wrapper's intentionally duplicated `@Deprecated` annotations.
// IGNORE_DEXING
// IGNORE_BACKEND: ANDROID
@file:OptIn(ExperimentalVersionOverloading::class)

@Deprecated("Versioned function with a hand-written deprecation", level = DeprecationLevel.WARNING)
fun versionedAndDeprecated(@IntroducedAt("1") x: Int = 0): Int = x

@Suppress("DEPRECATION")
fun box(): String {
    if (versionedAndDeprecated() != 0) return "fail1"

    return "OK"
}
