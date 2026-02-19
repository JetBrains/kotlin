// ARGUMENT: INV_1

// FILE: SomeClass.java

public class SomeClass {
    Integer something() { return 1; }
}

// FILE: main.kt

val x = SomeClass().something<caret_1>()

val y = <expr>arrayOf<String>()</expr>