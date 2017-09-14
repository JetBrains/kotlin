// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

inline header fun inlineFun()
header fun nonInlineFun()

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

<!IMPLEMENTATION_WITHOUT_HEADER!>impl fun inlineFun()<!> { }
impl fun nonInlineFun() { }

// MODULE: m3-js(m1-common)
// FILE: js.kt

impl <!NOTHING_TO_INLINE!>inline<!> fun inlineFun() { }
impl <!NOTHING_TO_INLINE!>inline<!> fun nonInlineFun() { }
