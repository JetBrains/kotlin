class Test {
    @`InnerAnnotation` <!REPEATED_ANNOTATION!>@InnerAnnotation<!>
    companion object : StaticClass(), <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER, MANY_CLASSES_IN_SUPERTYPE_LIST!>InnerClass<!>() {

    }

    annotation class InnerAnnotation
    open class StaticClass

    open inner class InnerClass
}
