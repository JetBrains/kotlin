// TARGET_BACKEND: JVM
// LAMBDAS: CLASS

import java.util.Date

fun assertGenericSuper(expected: String, function: Any?) {
    val clazz = (function as java.lang.Object).getClass()!!
    val genericSuper = clazz.getGenericInterfaces()[0]!!
    if ("$genericSuper" != expected)
        throw AssertionError("Fail, expected: $expected, actual: $genericSuper")
}


val unitFun = { }
val intFun = { 42 }
val stringParamFun = { x: String -> }
val listFun = { l: List<String> -> l }
val mutableListFun = fun (l: MutableList<Double>): MutableList<Int> = null!!
val funWithIn = fun (x: Comparable<String>) {}

val extensionFun = fun Any.() {}
val extensionWithArgFun = fun Long.(x: Any): Date = Date()

fun box(): String {
    assertGenericSuper("kotlin.jvm.functions.Function0<kotlin.Unit>", unitFun)
    assertGenericSuper("kotlin.jvm.functions.Function0<java.lang.Integer>", intFun)
    assertGenericSuper("kotlin.jvm.functions.Function1<java.lang.String, kotlin.Unit>", stringParamFun)
    assertGenericSuper("kotlin.jvm.functions.Function1<java.util.List<? extends java.lang.String>, java.util.List<? extends java.lang.String>>", listFun)
    assertGenericSuper("kotlin.jvm.functions.Function1<java.util.List<java.lang.Double>, java.util.List<java.lang.Integer>>", mutableListFun)
    assertGenericSuper("kotlin.jvm.functions.Function1<java.lang.Comparable<? super java.lang.String>, kotlin.Unit>", funWithIn)

    assertGenericSuper("kotlin.jvm.functions.Function1<java.lang.Object, kotlin.Unit>", extensionFun)
    assertGenericSuper("kotlin.jvm.functions.Function2<java.lang.Long, java.lang.Object, java.util.Date>", extensionWithArgFun)

    return "OK"
}
