class O
class K

JavaAnn(O::class, K::class) class MyClass

fun box(): String {
    val args = javaClass<MyClass>().getAnnotation(javaClass<JavaAnn>()).value()
    val argName1 = args[0].simpleName ?: "fail 1"
    val argName2 = args[1].simpleName ?: "fail 2"
    return argName1 + argName2
}
