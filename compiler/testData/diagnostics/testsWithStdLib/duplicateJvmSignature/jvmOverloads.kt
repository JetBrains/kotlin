// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    <!CONFLICTING_JVM_DECLARATIONS!>[kotlin.jvm.overloads] fun foo(s: String = "")<!> {
    }

    <!CONFLICTING_JVM_DECLARATIONS!>fun foo()<!> {
    }
}