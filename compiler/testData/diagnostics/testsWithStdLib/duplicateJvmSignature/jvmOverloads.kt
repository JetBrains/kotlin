// !DIAGNOSTICS: -UNUSED_PARAMETER
class <!CONFLICTING_JVM_DECLARATIONS!>A<!> {
    [kotlin.jvm.overloads] fun foo(s: String = "") {
    }

    <!CONFLICTING_JVM_DECLARATIONS!>fun foo()<!> {
    }
}