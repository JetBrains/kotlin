
fun box(): String {
    val ann = MyJavaClass::class.java.getAnnotation(JavaAnn::class.java)
    if (ann == null) return "fail: cannot find JavaAnn on MyClass"
    return ann.value.simpleName!!
}
