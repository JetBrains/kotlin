// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

inline platform fun inlineFun()
platform fun nonInlineFun()

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

<!PLATFORM_DEFINITION_WITHOUT_DECLARATION!>impl<!> fun inlineFun() { }
impl fun nonInlineFun() { }

// MODULE: m3-js(m1-common)
// FILE: js.kt

impl <!NOTHING_TO_INLINE!>inline<!> fun inlineFun() { }
<!PLATFORM_DEFINITION_WITHOUT_DECLARATION!>impl<!> <!NOTHING_TO_INLINE!>inline<!> fun nonInlineFun() { }
