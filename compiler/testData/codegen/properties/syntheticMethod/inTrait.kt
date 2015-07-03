annotation(retention = AnnotationRetention.RUNTIME) class SomeAnnotation(val value: String)

interface T {
    @SomeAnnotation("OK") val property: Int
}
