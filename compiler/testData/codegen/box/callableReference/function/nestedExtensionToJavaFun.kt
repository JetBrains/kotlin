// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: JavaClass.java
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public class JavaClass {
    public static String foo(Function2<A, Function2<A, Function1<A, String>, String>, String> a){
        return a.invoke(new A("FAIL"), new Function2<A, Function1<A, String>, String>() {
            @Override
            public String invoke(A innerA, Function1<A, String> func1) {
                return func1.invoke(new  A("OK"));
            }
        });
    }
}

// FILE: 1.kt
class A(public val a: String)

fun A.foo(block: A.(A.() -> String) -> String): String {
    return A("FAIL").block { a }
}

fun box(): String {
    return JavaClass.foo(A::foo)
}