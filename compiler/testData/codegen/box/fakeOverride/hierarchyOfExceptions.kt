// TARGET_BACKEND: JVM_IR
// ISSUE: KT-68571

// FILE: MyInterface.java
public interface MyInterface {
    String getMessage();
}

// FILE: MyException.java
public class MyException extends RuntimeException implements MyInterface {
    public MyException(String message) {
        super(message);
    }
}

// FILE: MyOtherException.java
public class MyOtherException extends RuntimeException implements MyInterface {
    public MyOtherException(String message) {
        super(message);
    }
}

// FILE: main.kt
fun box(): String {
    try {
        throw MyException("OK")
    }
    catch (e: Throwable) {
        return when (e) {
            is MyException -> "${e.message}"
            is MyOtherException -> "MyOtherException: ${e.message}"
            else -> "Unknown exception: ${e.message}"
        }
    }
    return "No Exception"
}
