annotation(retention = AnnotationRetention.RUNTIME) class SomeAnnotation(val value: String)

@SomeAnnotation("OK") val property: Int
    get() = 42
