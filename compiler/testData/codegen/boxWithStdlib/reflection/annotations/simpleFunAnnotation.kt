annotation(retention = AnnotationRetention.RUNTIME) class Simple(val value: String)

@Simple("OK")
fun box(): String {
    return (::box.annotations.single() as Simple).value
}
