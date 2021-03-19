// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FULL_JDK
// ISSUE: KT-45584

// FILE: PlaceholderExceptionSupport.java

public interface PlaceholderExceptionSupport {
    String getMessage();
}

// FILE: PlaceholderException.java

public class PlaceholderException extends RuntimeException implements PlaceholderExceptionSupport {}

// FILE: main.kt

class KotlinTestFailure : PlaceholderException() {} // <-- CONFLICTING_INHERITED_JVM_DECLARATIONS

fun box(): String = "OK"
