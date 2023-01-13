// JDK_RELEASE: 14

// FILE: Example.java

public class Example {
    public static final String MESSAGE = "OK";
}

// FILE: Kotlin.kt

fun box(): String {
    return Example.MESSAGE
}