// TARGET_BACKEND: JVM
// WITH_COROUTINES
// WITH_STDLIB

suspend fun id(x: String): String = x

val f: suspend (String) -> String = ::id

fun box(): String {
    val actual = f.javaClass.genericInterfaces.toList().toString()
    val expected = "[" +
        "kotlin.jvm.functions.Function2<java.lang.String, kotlin.coroutines.Continuation<? super java.lang.String>, java.lang.Object>, " +
        "interface kotlin.coroutines.jvm.internal.SuspendFunction" +
    "]"

    return if (expected == actual) "OK" else "Fail: $actual"
}
