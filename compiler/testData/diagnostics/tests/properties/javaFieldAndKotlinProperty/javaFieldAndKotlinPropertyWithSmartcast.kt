// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

// FILE: Jaba.java
public class Jaba {
    public String a = "O";
    public String b = "";
}

// FILE: test.kt
class My : Jaba() {
    private val a: String = "FAIL"
    private val b: String = "FAIL"
}

fun test(j: Any): String {
    if (j is My) {
        <!DEBUG_INFO_SMARTCAST!>j<!>.b = "K"
        return <!DEBUG_INFO_SMARTCAST!>j<!>.a + <!DEBUG_INFO_SMARTCAST!>j<!>.b
    }
    return "NO SMARTCAST"
}

fun box(): String = test(My())
