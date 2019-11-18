// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: AT.java

public class AT<G> {
    public String result = "fail";
    public void foo(G ...y) {
        result = "OK";
    }
}

// FILE: main.kt

fun AT<*>.bar() {
    foo()
}

fun box(): String {
    val a = AT<String>()
    a.bar()
    return a.result
}
