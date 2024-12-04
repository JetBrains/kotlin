// ENABLE_JVM_PREVIEW

// FILE: R.java

public record R<T>(T value) {}

// FILE: test.kt

fun box(): String {
    val r = R("OK")
    if (r.value != "OK") return "FAIL"
    if (run(r::value) != "OK") return "FAIL"
    if (r.let(R<String>::value) != "OK") return "FAIL"

    return "OK"
}
