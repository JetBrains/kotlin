class O
class K

JavaAnn(args = array(O::class, K::class)) class MyClass

fun box(): String {
    val args = javaClass<MyClass>().getAnnotation(javaClass<JavaAnn>()).args()
    val argName1 = args[0].simpleName ?: "fail 1"
    val argName2 = args[1].simpleName ?: "fail 2"
    return argName1 + argName2
}
