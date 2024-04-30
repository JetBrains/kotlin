// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

// FILE: Jaba.java
public class Jaba {
    public String a = "OK";
}

// FILE: test.kt
class My : Jaba() {
    private val a: String = "FAIL"

    operator fun plus(my: My) = my
}

fun create(): My? = My()

fun box(): String {
    return (create() ?: My()).a
}
