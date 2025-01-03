// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Base<!> {
    fun foo(a: Int): String
    val a : String
}

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>ExtensionBase<!> {
    fun Int.foo(): String
    val Int.a : String
}

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> : Base

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!> : ExtensionBase

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class A<!> : Base {
    <!NOTHING_TO_OVERRIDE!>override<!> fun Int.foo(): String {
        return ""
    }
    <!NOTHING_TO_OVERRIDE!>override<!> val Int.a: String
        get() = ""
}

actual <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class B<!> : ExtensionBase {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(a: Int): String {
        return ""
    }
    <!NOTHING_TO_OVERRIDE!>override<!> val a: String = ""
}
