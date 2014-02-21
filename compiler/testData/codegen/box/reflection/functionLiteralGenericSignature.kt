import java.util.Date

fun assertGenericSuper(expected: String, function: Any?) {
    val clazz = (function as java.lang.Object).getClass()!!
    val genericSuper = clazz.getGenericSuperclass()!!
    if ("$genericSuper" != expected)
        throw AssertionError("Fail, expected: $expected, actual: $genericSuper")
}


val unitFun = { }
val intFun = { 42 }
val stringParamFun = { (x: String) : Unit -> }
val listFun = { (l: List<String>) : List<String> -> l }
val mutableListFun = { (l: MutableList<Double>) : MutableList<Int> -> null!! }
val funWithIn = { (x: Comparable<String>) : Unit -> }

val extensionFun = { Any.() : Unit -> }
val extensionWithArgFun = { Long.(x: Any) : Date -> Date() }

fun box(): String {
    assertGenericSuper("kotlin.FunctionImpl0<kotlin.Unit>", unitFun)
    assertGenericSuper("kotlin.FunctionImpl0<java.lang.Integer>", intFun)
    assertGenericSuper("kotlin.FunctionImpl1<java.lang.String, kotlin.Unit>", stringParamFun)
    assertGenericSuper("kotlin.FunctionImpl1<java.util.List<? extends java.lang.String>, java.util.List<? extends java.lang.String>>", listFun)
    assertGenericSuper("kotlin.FunctionImpl1<java.util.List<java.lang.Double>, java.util.List<java.lang.Integer>>", mutableListFun)
    assertGenericSuper("kotlin.FunctionImpl1<java.lang.Comparable<? super java.lang.String>, kotlin.Unit>", funWithIn)

    assertGenericSuper("kotlin.ExtensionFunctionImpl0<java.lang.Object, kotlin.Unit>", extensionFun)
    assertGenericSuper("kotlin.ExtensionFunctionImpl1<java.lang.Long, java.lang.Object, java.util.Date>", extensionWithArgFun)

    return "OK"
}
