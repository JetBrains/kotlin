// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    <!CONFLICTING_JVM_DECLARATIONS!>@kotlin.jvm.JvmOverloads fun foo(s: String = "")<!> {
    }

    <!CONFLICTING_JVM_DECLARATIONS!>fun foo()<!> {
    }
}