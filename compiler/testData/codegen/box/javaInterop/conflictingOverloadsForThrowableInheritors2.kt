// TARGET_BACKEND: JVM
// FULL_JDK
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-61805
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
