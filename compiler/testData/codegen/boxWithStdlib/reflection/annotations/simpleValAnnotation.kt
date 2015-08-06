annotation(retention = AnnotationRetention.RUNTIME) class Simple(val value: String)

@Simple("OK")
val foo: Int = 0

fun box(): String {
    return (::foo.annotations.single() as Simple).value
}
