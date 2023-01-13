// JDK_RELEASE: 9

// FILE: Example.java

public class Example {
    public static final String MESSAGE = "OK";
}

// FILE: Kotlin.kt

fun box(): String {
    return Example.MESSAGE
}