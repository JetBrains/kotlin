// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C1<!>()
expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I1<!>

open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Common1_1<!><!> : C1(), I1
open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Common1_2<!><!> : I1, C1()

expect open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Expect1_1<!><!> : C1, I1
expect open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Expect1_2<!><!> : I1, C1


expect abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C2<!>()
expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I2<!>

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Common2_1<!> : C2(), I2
open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Common2_2<!> : I2, C2()

expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Expect2_1<!> : C2, I2
expect open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Expect2_2<!> : I2, C2

// MODULE: m1-jvm()()(m1-common)
// FILE: main.kt

actual open class C1 {
    fun f() {}
}

actual interface I1 {
    fun f() {}
}

actual open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Expect1_1<!> : C1(), I1
actual open <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Expect1_2<!> : I1, C1()


actual abstract class C2 actual constructor() {
    fun g() {}
}

actual interface I2 {
    fun g()
}

actual open class Expect2_1 : C2(), I2
actual open class Expect2_2 : I2, C2()
