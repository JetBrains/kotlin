// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: BaseJava.java
import java.util.List;

public class BaseJava {
    public static <T extends List<String>> String foo(T a) {
        return a.get(0);
    }
}

// FILE: test.kt
fun <T: List<*>> foo(x: T.()-> String, y: T): String {
    return  x(y)
}

fun box(): String {
    return foo(BaseJava::foo, listOf("OK"))
}