// TARGET_BACKEND: JVM
// WITH_STDLIB

import java.util.Date

fun assertEquals(expected: String, actual: Any) {
    if ("$actual" != expected)
        throw AssertionError("Fail, expected: $expected, actual: $actual")
}

fun assertGenericSuper(expected: String, function: Any?) {
    val clazz = (function as java.lang.Object).getClass()!!
    val genericSuper = clazz.getGenericInterfaces()[0]!!
    assertEquals(expected, genericSuper)
}

val unitFun = suspend { }
val intFun = suspend { 42 }

fun assertStringParamFun(lambda: suspend (String) -> Unit) {
    assertEquals(
        "kotlin.jvm.functions.Function2<java.lang.String, kotlin.coroutines.Continuation<? super kotlin.Unit>, java.lang.Object>",
        lambda.javaClass.genericInterfaces.single()
    )
}

fun assertListFun(lambda: suspend (List<String>) -> Unit) {
    assertEquals(
        "kotlin.jvm.functions.Function2<java.util.List<? extends java.lang.String>, kotlin.coroutines.Continuation<? super kotlin.Unit>, java.lang.Object>",
        lambda.javaClass.genericInterfaces.single()
    )
}

fun assertMutableListFun(lambda: suspend (MutableList<Double>) -> MutableList<Int>) {
    assertEquals(
        "kotlin.jvm.functions.Function2<java.util.List<java.lang.Double>, kotlin.coroutines.Continuation<? super java.util.List<java.lang.Integer>>, java.lang.Object>",
        lambda.javaClass.genericInterfaces.single()
    )
}

fun assertFunWithIn(lambda: suspend (Comparable<String>) -> Unit) {
    assertEquals(
        "kotlin.jvm.functions.Function2<java.lang.Comparable<? super java.lang.String>, kotlin.coroutines.Continuation<? super kotlin.Unit>, java.lang.Object>",
        lambda.javaClass.genericInterfaces.single()
    )
}

fun assertExtensionFun(lambda: suspend Any.() -> Unit) {
    assertEquals(
        "kotlin.jvm.functions.Function2<java.lang.Object, kotlin.coroutines.Continuation<? super kotlin.Unit>, java.lang.Object>",
        lambda.javaClass.genericInterfaces.single()
    )
}

fun assertExtensionWithArgFun(lambda: suspend Long.(x: Any) -> Date) {
    assertEquals(
        "kotlin.jvm.functions.Function3<java.lang.Long, java.lang.Object, kotlin.coroutines.Continuation<? super java.util.Date>, java.lang.Object>",
        lambda.javaClass.genericInterfaces.single()
    )
}

fun box(): String {
    assertGenericSuper("kotlin.jvm.functions.Function1<kotlin.coroutines.Continuation<? super kotlin.Unit>, java.lang.Object>", unitFun)
    assertGenericSuper("kotlin.jvm.functions.Function1<kotlin.coroutines.Continuation<? super java.lang.Integer>, java.lang.Object>", intFun)
    assertStringParamFun { x: String -> }
    assertListFun { l: List<String> -> l }
    assertMutableListFun { l -> null!! }
    assertFunWithIn { x -> }
    assertExtensionFun { }
    assertExtensionWithArgFun { x -> Date() }
    return "OK"
}
