class Klass

fun box(): String {
    val x = Klass::class
    if (x.simpleName != "Klass") return "Fail x: ${x.simpleName}"

    val y = java.util.Date::class
    if (y.simpleName != "Date") return "Fail y: ${y.simpleName}"

    val z = kotlin.jvm.internal.KotlinSyntheticClass.Kind::class
    if (z.simpleName != "Kind") return "Fail z: ${z.simpleName}"

    return "OK"
}
