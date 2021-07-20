// TARGET_BACKEND: JVM
// Ignored cause of KT-47542
// IGNORE_BACKEND: JVM, JVM_IR
// FULL_JDK
// ISSUE: KT-45584

// FILE: PlaceholderExceptionSupport.java

public interface PlaceholderExceptionSupport {
    String getMessage();
}

// FILE: PlaceholderException.java

public class PlaceholderException extends RuntimeException implements PlaceholderExceptionSupport {
    public PlaceholderException(String x) { super(x); }
}

// FILE: main.kt

class KotlinTestFailure : PlaceholderException("OK") {} // <-- CONFLICTING_INHERITED_JVM_DECLARATIONS

fun box(): String = KotlinTestFailure().message ?: "fail"
