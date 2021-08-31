// FIR_IDENTICAL
// FULL_JDK
// ISSUE: KT-47542

// FILE: PlaceholderExceptionSupport.java

public interface PlaceholderExceptionSupport {
    String getMessage();
}

// FILE: ExceptionWithAbstractMessage.java

public class ExceptionWithAbstractMessage extends RuntimeException implements PlaceholderExceptionSupport {
    public ExceptionWithAbstractMessage(String x) { super(x); }

    abstract String getMessage();
}

// FILE: PlaceholderException.java

public class PlaceholderException extends RuntimeException implements PlaceholderExceptionSupport {
    public PlaceholderException(String x) { super(x); }
}

// FILE: main.kt
class KotlinTestSuccess : PlaceholderException("OK") {}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class KotlinTestFailure<!> : ExceptionWithAbstractMessage("FAIL") {}