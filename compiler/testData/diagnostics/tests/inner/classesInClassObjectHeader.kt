class Test {
    [`InnerAnnotation`InnerAnnotation]
    default object : StaticClass(), <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!><!MANY_CLASSES_IN_SUPERTYPE_LIST!>InnerClass<!>()<!> {

    }

    annotation class InnerAnnotation
    open class StaticClass

    open inner class InnerClass
}