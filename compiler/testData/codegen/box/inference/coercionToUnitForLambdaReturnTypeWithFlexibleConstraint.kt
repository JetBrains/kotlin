// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: TestJ.java

public class TestJ {
    public static <T> In<T> materialize() {
        return null;
    }
}

// FILE: test.kt

class In<in T>

fun <T> inferred(e: In<T>?, l: () -> T): T = l()

fun box(): String {
    // coercion to Unit for T!
    val inferred = (inferred(TestJ.materialize<Unit>(), { null })).toString()
    return if (inferred == "kotlin.Unit") "OK" else "fail : $inferred"
}