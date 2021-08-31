// ISSUE: KT-41215, KT-43551

// FILE: Base.java
public sealed class Base permits A, B {}

// FILE: A.java
public final class A extends Base {}

// FILE: B.java
public sealed class B extends Base permits B.C, B.D {
    public static final class C extends B {}

    public static non-sealed class D extends B {}
}

// FILE: main.kt
fun test_1(base: Base): String {
    return when (base) {
        is A -> "O"
        is B -> "Fail"
    }
}

fun test_2(base: Base): String {
    return when (base) {
        is A -> "Fail A"
        is B.C -> "K"
        is B.D -> "Fail B.D"
    }
}

fun box(): String {
    return test_1(A()) + test_2(B.C())
}
