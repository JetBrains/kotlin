// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-70179

// MODULE: m1-common
// FILE: common.kt

expect annotation class Ann()

// MODULE: m2-jvm
// FILE: some/my/Ann.java

package some.my;

public @interface Ann {}

// MODULE: m3-jvm(m2-jvm)()(m1-common)
// FILE: Ann.kt

actual typealias Ann = some.my.Ann

// MODULE: m4-jvm(m3-jvm)
// FILE: test.kt

@Ann
fun foo() {}
