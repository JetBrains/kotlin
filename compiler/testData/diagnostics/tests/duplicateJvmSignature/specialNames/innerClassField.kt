// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    inner class <!CONFLICTING_JVM_DECLARATIONS!>D<!> {
        <!CONFLICTING_JVM_DECLARATIONS!>val `this$0`: C?<!> = null
    }
}