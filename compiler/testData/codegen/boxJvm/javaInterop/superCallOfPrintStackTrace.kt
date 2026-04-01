// TARGET_BACKEND: JVM
// FULL_JDK
// FILE: PlaceholderException.java

public class PlaceholderException extends RuntimeException {}

// FILE: main.kt
import java.io.PrintWriter

class KotlinTestFailure : PlaceholderException() {
    override fun printStackTrace(s: PrintWriter?) {
        super.printStackTrace(s)
    }
}

fun box(): String = "OK"
