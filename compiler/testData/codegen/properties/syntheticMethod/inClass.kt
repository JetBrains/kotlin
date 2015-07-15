annotation(retention = AnnotationRetention.RUNTIME) class SomeAnnotation(val value: String)

class A {
    @SomeAnnotation("OK") val property: Int
        get() = 42
}
