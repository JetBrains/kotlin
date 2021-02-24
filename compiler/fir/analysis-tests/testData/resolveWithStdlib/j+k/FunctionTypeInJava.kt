// WITH_RUNTIME
// FILE: JavaClass.java
public class JavaClass {
    public static void foo1(kotlin.jvm.functions.Function0<Integer> x) {}
    public static void foo2(kotlin.jvm.functions.Function1<Integer, String> x) {}
    public static <T> void foo3(kotlin.jvm.functions.Function1<T, String> x, T y) {}
}

// FILE: main.kt

fun main() {
    JavaClass.foo1 { 123 }


    JavaClass.foo2 { (it + 2).toString() }
    JavaClass.foo2({ (it + 3).toString() })
    val y = { x: Int -> x.toString() }
    JavaClass.foo2(y)


    JavaClass.foo3({ (it + 4).toString() }, 5)
}
