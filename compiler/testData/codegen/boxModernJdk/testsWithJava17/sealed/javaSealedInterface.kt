// ISSUE: KT-41215, KT-43551

// FILE: Base.java
public sealed interface Base permits A, B, E {}

// FILE: A.java
public non-sealed interface A extends Base {}

// FILE: B.java
public sealed interface B extends Base permits B.C, B.D {
    public static final class C implements B {}

    public static non-sealed interface D extends B {}
}

// FILE: E.java
public enum E implements Base {
    First, Second
}

// FILE: main.kt
fun test_1(base: Base): String {
    return when (base) {
        is A -> "Fail A"
        is B -> "Fail B"
        is E -> "O"
    }
}

fun test_2(base: Base): String {
    return when (base) {
        is A -> "Fail A"
        is B.C -> "Fail B.C"
        is B.D -> "K"
        E.First -> "Fail E.First"
        E.Second -> "Fail E.Second"
    }
}

class MyD : B.D

fun box(): String {
    return test_1(E.First) + test_2(MyD())
}
