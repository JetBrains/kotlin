// FILE: JavaEnum.java

public enum JavaEnum {
    FIRST,
    SECOND,
    LAST;
}

// FILE: test.kt

fun print(s: String) {}

fun foo(je: JavaEnum) {
    when (je) {
        JavaEnum.FIRST -> print("1")
        JavaEnum.SECOND -> print("2")
        JavaEnum.LAST -> print("10")
    }
}
