annotation(retention = AnnotationRetention.RUNTIME) class Simple(val value: String)

@Simple("OK")
class A

fun box(): String {
    return (A::class.annotations.single() as Simple).value
}
