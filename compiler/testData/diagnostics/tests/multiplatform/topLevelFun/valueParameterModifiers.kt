// !LANGUAGE: +MultiPlatformProjects
// !DIAGNOSTICS: -NOTHING_TO_INLINE
// MODULE: m1-common
// FILE: common.kt

header fun f1(s: () -> String)
header inline fun f2(s: () -> String)
header inline fun f3(noinline s: () -> String)

header fun f4(s: () -> String)
header inline fun f5(s: () -> String)
header inline fun f6(crossinline s: () -> String)

header fun f7(x: Any)
header fun f8(vararg x: Any)

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl inline fun f1(noinline s: () -> String) {}
<!IMPLEMENTATION_WITHOUT_HEADER!>impl inline fun f2(noinline s: () -> String)<!> {}
impl inline fun f3(s: () -> String) {}
impl inline fun f4(crossinline s: () -> String) {}
<!IMPLEMENTATION_WITHOUT_HEADER!>impl inline fun f5(crossinline s: () -> String)<!> {}
impl inline fun f6(s: () -> String) {}
<!IMPLEMENTATION_WITHOUT_HEADER!>impl fun f7(vararg x: Any)<!> {}
<!IMPLEMENTATION_WITHOUT_HEADER!>impl fun f8(x: Any)<!> {}
