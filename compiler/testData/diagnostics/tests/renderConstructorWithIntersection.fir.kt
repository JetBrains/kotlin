// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-70194
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: Flexier.java

public class Flexier {
    public static <T> T flexify(T it) {
        return it;
    }
}

// FILE: Main.kt

interface A
interface B

object O1 : A, B
object O2 : A, B

fun test() {
    val a: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>Flexier.flexify(if (true) O1 else O2)<!>
}
