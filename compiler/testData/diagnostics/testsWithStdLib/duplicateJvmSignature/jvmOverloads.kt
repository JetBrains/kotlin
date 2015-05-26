// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    <!CONFLICTING_JVM_DECLARATIONS!>@kotlin.jvm.jvmOverloads fun foo(s: String = "")<!> {
    }

    <!CONFLICTING_JVM_DECLARATIONS!>fun foo()<!> {
    }
}