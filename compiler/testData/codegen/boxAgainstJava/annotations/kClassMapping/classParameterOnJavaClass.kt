
fun box(): String {
    val ann = javaClass<MyJavaClass>().getAnnotation(javaClass<JavaAnn>())
    if (ann == null) return "fail: cannot find JavaAnn on MyClass"
    return ann.value().simpleName!!
}
