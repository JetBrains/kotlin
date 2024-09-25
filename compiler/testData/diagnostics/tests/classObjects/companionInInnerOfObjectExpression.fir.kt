// LANGUAGE: -ForbidCompanionInLocalInnerClass
val x = object {
    inner class D {
        companion <!NESTED_CLASS_NOT_ALLOWED_IN_LOCAL_WARNING!>object<!>
    }
}