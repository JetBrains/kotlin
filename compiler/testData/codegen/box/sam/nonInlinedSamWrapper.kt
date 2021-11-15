// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: Test.java

public class Test {
    public static void run(Runnable r) {
        r.run();
    }
}

// FILE: test.kt

fun runFunction1(x: () -> Unit) = Runnable(x).run()
fun runFunction2(x: () -> Unit) = Test.run(x)

fun box(): String {
    var result = ""
    runFunction1 { result += "O" }
    runFunction2 { result += "K" }
    return result
}
