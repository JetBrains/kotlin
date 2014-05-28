// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!CONFLICTING_PLATFORM_DECLARATIONS!>inner class D<!> {
        <!CONFLICTING_PLATFORM_DECLARATIONS!>val `this$0`: C?<!> = null
    }
}