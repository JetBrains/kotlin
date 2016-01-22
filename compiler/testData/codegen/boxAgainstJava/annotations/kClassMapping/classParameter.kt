
class OK

@JavaAnn(OK::class) class MyClass

fun box(): String {
    val ann = MyClass::class.java.getAnnotation(JavaAnn::class.java)
    if (ann == null) return "fail: cannot find JavaAnn on MyClass"
    return ann.value.simpleName!!
}
