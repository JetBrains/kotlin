// TARGET_BACKEND: JVM_IR

// FILE: JavaEnum.java

public enum JavaEnum {
    A, B;
    public final int ordinal = 1;
}

// FILE: box.kt

fun box(): String {
    return when (JavaEnum.A) {
        JavaEnum.A -> "OK"
        JavaEnum.B -> "Fail"
    }
}