class Test {
    @`InnerAnnotation` <!REPEATED_ANNOTATION!>@InnerAnnotation<!>
    companion <!CYCLIC_SCOPES_WITH_COMPANION, CYCLIC_SCOPES_WITH_COMPANION!>object<!> : StaticClass(), <!UNRESOLVED_REFERENCE, MANY_CLASSES_IN_SUPERTYPE_LIST, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>InnerClass<!>() {

    }

    annotation class InnerAnnotation
    open class <!CYCLIC_SCOPES_WITH_COMPANION!>StaticClass<!>

    open inner class <!CYCLIC_SCOPES_WITH_COMPANION!>InnerClass<!>
}