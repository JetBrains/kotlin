JavaAnn("value") class MyClass

fun box(): String {
    val ann = javaClass<MyClass>().getAnnotation(javaClass<JavaAnn>())
    if (ann == null) return "fail: cannot find Ann on MyClass}"
    if (ann.value() != "value") return "fail: annotation parameter i should be 'value', but was ${ann.value()}"
    return "OK"
}
