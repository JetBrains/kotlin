// !WITH_NEW_INFERENCE
// !LANGUAGE: +ExpectedTypeFromCast
// !DIAGNOSTICS: -UNUSED_VARIABLE -DEBUG_INFO_LEAKING_THIS

// FILE: a/View.java
package a;

public class View {

}

// FILE: a/Test.java
package a;

public class Test {
    public <T extends View> T findViewById(int id);
}

// FILE: 1.kt
package a


class X : View()

class Y<T> : View()

val xExplicit: X = Test().findViewById(0)
val xCast = Test().findViewById(0) as X

val xCastExplicitType = Test().findViewById<X>(0) as X
val xSafeCastExplicitType = Test().findViewById<X>(0) <!USELESS_CAST!>as? X<!>

val yExplicit: Y<String> = Test().findViewById(0)
val yCast = Test().findViewById(0) as Y<String>


class TestChild : Test() {
    val xExplicit: X = findViewById(0)
    val xCast = findViewById(0) as X

    val yExplicit: Y<String> = findViewById(0)
    val yCast = findViewById(0) as Y<String>
}

fun test(t: Test) {
    val xExplicit: X = t.findViewById(0)
    val xCast = t.findViewById(0) as X

    val yExplicit: Y<String> = t.findViewById(0)
    val yCast = t.findViewById(0) as Y<String>
}

fun test2(t: Test?) {
    val xSafeCallSafeCast = t?.findViewById(0) as? X
    val xSafeCallSafeCastExplicitType = t?.findViewById<X>(0) <!USELESS_CAST!>as? X<!>

    val xSafeCallCast = t?.findViewById(0) as X
    val xSafeCallCastExplicitType = t<!UNNECESSARY_SAFE_CALL!>?.<!>findViewById<X>(0) as X
}
