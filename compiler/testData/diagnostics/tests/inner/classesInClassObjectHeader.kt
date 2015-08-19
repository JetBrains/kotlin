class Test {
    @`InnerAnnotation` <!REPEATED_ANNOTATION!>@InnerAnnotation<!>
    companion object : StaticClass(), <!UNRESOLVED_REFERENCE, MANY_CLASSES_IN_SUPERTYPE_LIST, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>InnerClass<!>() {

    }

    annotation class InnerAnnotation
    open class StaticClass

    open inner class InnerClass
}