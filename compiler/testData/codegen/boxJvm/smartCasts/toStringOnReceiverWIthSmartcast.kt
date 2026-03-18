// TARGET_BACKEND: JVM_IR
// FILE: A.java
public class A {
    @Override
    public String toString() {
        return "O";
    }
}

// FILE: B.java
public class B {
    @Override
    public String toString() {
        return "K";
    }
}

// FILE: main.kt
fun test(x: Any): String {
    return when (x) {
        is A -> x.toString()
        is B -> x.toString()
        else -> "fail"
    }
}

fun box(): String {
    return test(A()) + test(B())
}
