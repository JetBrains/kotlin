@Retention(AnnotationRetention.RUNTIME)
annotation class SomeAnnotation(val value: String)

@SomeAnnotation("OK") val property: Int
    get() = 42
