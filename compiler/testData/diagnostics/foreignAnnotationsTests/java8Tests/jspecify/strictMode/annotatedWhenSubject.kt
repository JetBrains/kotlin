// ISSUE: KT-84106
// JSPECIFY_STATE: strict

// FILE: J.java
import org.jspecify.annotations.*;

public class J {
    @Nullable
    public static <T> T identity(T t) { return null; }
}

// FILE: test.kt
sealed class Sealed1
class C1 : Sealed1()

sealed class Sealed2
object O2 : Sealed2()

fun test() {
    <!NO_ELSE_IN_WHEN!>when<!> (J.identity(C1() as Sealed1)) {
        is C1 -> {}
    }

    when (J.identity(C1() as Sealed1)) {
        is C1 -> {}
        null -> {}
    }

    <!NO_ELSE_IN_WHEN!>when<!> (J.identity(C1() as Sealed1)) {
        is C1? -> {}
    }

    <!NO_ELSE_IN_WHEN!>when<!> (J.identity(O2 as Sealed2)) {
        O2 -> {}
    }

    when (J.identity(O2 as Sealed2)) {
        O2 -> {}
        null -> {}
    }
}
