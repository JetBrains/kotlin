// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!CONFLICTING_JVM_DECLARATIONS!>inner class D<!> {
        <!CONFLICTING_JVM_DECLARATIONS!>val `this$0`: C?<!> = null
    }
}