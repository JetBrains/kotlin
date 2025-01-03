// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect <!EXPECTED_EXTERNAL_DECLARATION, EXPECTED_EXTERNAL_DECLARATION{JVM}!>external<!> fun foo()<!>
<!CONFLICTING_OVERLOADS!>expect fun bar()<!>

expect <!EXPECTED_EXTERNAL_DECLARATION, EXPECTED_EXTERNAL_DECLARATION{JVM}, WRONG_MODIFIER_TARGET{JVM}!>external<!> var <!REDECLARATION!>prop<!>: String

expect var <!REDECLARATION!>getAndSet<!>: String
    <!EXPECTED_EXTERNAL_DECLARATION, EXPECTED_EXTERNAL_DECLARATION{JVM}!>external<!> get
    <!EXPECTED_EXTERNAL_DECLARATION, EXPECTED_EXTERNAL_DECLARATION{JVM}!>external<!> set

<!EXPECTED_EXTERNAL_DECLARATION, EXPECTED_EXTERNAL_DECLARATION{JVM}, WRONG_MODIFIER_TARGET{JVM}!>external<!> expect val <!REDECLARATION!>explicitGetter<!>: String
    <!EXPECTED_EXTERNAL_DECLARATION, EXPECTED_EXTERNAL_DECLARATION{JVM}!>external<!> get

expect <!EXPECTED_EXTERNAL_DECLARATION, EXPECTED_EXTERNAL_DECLARATION{JVM}, WRONG_MODIFIER_TARGET{JVM}!>external<!> class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {
    <!EXPECTED_EXTERNAL_DECLARATION, EXPECTED_EXTERNAL_DECLARATION{JVM}!>external<!> fun foo()
    fun bar()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual external fun foo()
actual external fun bar()

actual <!WRONG_MODIFIER_TARGET!>external<!> var prop: String

actual var getAndSet: String
    external get
    external set

actual <!WRONG_MODIFIER_TARGET!>external<!> val explicitGetter: String
    external get

actual class A {
    actual external fun foo()
    actual external fun bar()
}
