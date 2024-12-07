@Retention(AnnotationRetention.BINARY)
annotation class MyAnnotation

interface MyInterface {
    @MyAnnotation
    fun foo() {
    }
}

interface I1 : MyInterface

interface I2 : MyInterface {
    override fun foo() {}
}

class MyClass : I1, I2
