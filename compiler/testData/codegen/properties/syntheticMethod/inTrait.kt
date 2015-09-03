@Retention(AnnotationRetention.RUNTIME)
annotation class SomeAnnotation(val value: String)

interface T {
    @SomeAnnotation("OK") val property: Int
}
