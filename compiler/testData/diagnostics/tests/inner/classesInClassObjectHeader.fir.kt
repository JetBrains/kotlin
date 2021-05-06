class Test {
    @`InnerAnnotation` @InnerAnnotation
    companion object : StaticClass(), <!MANY_CLASSES_IN_SUPERTYPE_LIST, UNRESOLVED_REFERENCE!>InnerClass<!>() {

    }

    annotation class InnerAnnotation
    open class StaticClass

    open inner class InnerClass
}
