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

val extensionFun = { Any.() : Unit -> }
val extensionWithArgFun = { Long.(x: Any) : Date -> Date() }

fun box(): String {
    assertGenericSuper("jet.FunctionImpl0<? extends jet.Unit>", unitFun)
    assertGenericSuper("jet.FunctionImpl0<? extends java.lang.Integer>", intFun)
    assertGenericSuper("jet.FunctionImpl1<? super java.lang.String, ? extends jet.Unit>", stringParamFun)
    assertGenericSuper("jet.FunctionImpl1<? super java.util.List<? extends java.lang.String>, ? extends java.util.List<? extends java.lang.String>>", listFun)
    assertGenericSuper("jet.FunctionImpl1<? super java.util.List<java.lang.Double>, ? extends java.util.List<java.lang.Integer>>", mutableListFun)
    
    assertGenericSuper("jet.ExtensionFunctionImpl0<? super java.lang.Object, ? extends jet.Unit>", extensionFun)
    assertGenericSuper("jet.ExtensionFunctionImpl1<? super java.lang.Long, ? super java.lang.Object, ? extends java.util.Date>", extensionWithArgFun)
    
    return "OK"
}
