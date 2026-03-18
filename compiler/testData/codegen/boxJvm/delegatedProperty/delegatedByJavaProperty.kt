// WITH_STDLIB
// TARGET_BACKEND: JVM
// FILE: JavaClass.java
import kotlin.jvm.functions.Function1;

public class JavaClass {
    public static Function1<String, String> a = (s) -> s;
}
// FILE: test.kt
val String.b: String.() -> String by JavaClass::a

fun box(): String {
    val a: String.() -> String by JavaClass::a
    return "FAIL".b("O") +"K".a()
}