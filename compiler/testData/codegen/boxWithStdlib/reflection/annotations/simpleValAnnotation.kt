annotation(retention = AnnotationRetention.RUNTIME) class Simple(val value: String)

@property:Simple("OK")
val foo: Int = 0

fun box(): String {
    return (::foo.annotations.single() as Simple).value
}
