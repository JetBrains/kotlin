annotation(retention = AnnotationRetention.RUNTIME) class Simple(val value: String)

fun test(@Simple("OK") x: Int) {}

fun box(): String {
    return (::test.parameters.single().annotations.single() as Simple).value
}
