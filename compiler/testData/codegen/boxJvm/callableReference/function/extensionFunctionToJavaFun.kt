// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: JavaClass.java
import kotlin.jvm.functions.Function1;

public class JavaClass {
    public static String foo(Function1<A, String> a){
        return a.invoke(new A("OK"));
    }
}

// FILE: 1.kt
class A(val a: String)

fun A.bar(): String {
    return this.a
}

fun box(): String {
    return JavaClass.foo(A::bar)
}
