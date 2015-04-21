
class OK

JavaAnn(OK::class) class MyClass

fun box(): String {
    val ann = javaClass<MyClass>().getAnnotation(javaClass<JavaAnn>())
    if (ann == null) return "fail: cannot find JavaAnn on MyClass"
    return ann.value().simpleName!!
}
