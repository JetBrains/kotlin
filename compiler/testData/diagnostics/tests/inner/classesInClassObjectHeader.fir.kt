class Test {
    @`InnerAnnotation` @InnerAnnotation
    companion object : StaticClass(), InnerClass() {

    }

    annotation class InnerAnnotation
    open class StaticClass

    open inner class InnerClass
}