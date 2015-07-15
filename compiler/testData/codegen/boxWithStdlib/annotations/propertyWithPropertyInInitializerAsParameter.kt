Ann(i) class MyClass

fun box(): String {
    val ann = javaClass<MyClass>().getAnnotation(javaClass<Ann>())
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.i != 1) return "fail: annotation parameter i should be 1, but was ${ann.i}"
    return "OK"
}

annotation(retention = AnnotationRetention.RUNTIME) class Ann(val i: Int)

val i2: Int = 1
val i: Int = i2
