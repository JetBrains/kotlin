// TARGET_BACKEND: JVM
// FILE: BaseImplJava.java
public class BaseImplJava implements Base {
    @Override
    public String foo(int a) {
        return "O";
    }

    @Override
    public String getA(int a) {
        return "K";
    }
}

// FILE: test.kt
interface Base {
    fun Int.foo(): String
    val Int.a : String
}

fun box(): String {
    val a = BaseImplJava()
    with(a) {
        return 1.foo() + 1.a
    }
}