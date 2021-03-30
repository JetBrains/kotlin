class Test {
    @`InnerAnnotation` @InnerAnnotation
    companion object : StaticClass(), <!UNRESOLVED_REFERENCE!>InnerClass<!>() {

    }

    annotation class InnerAnnotation
    open class StaticClass

    open inner class InnerClass
}
