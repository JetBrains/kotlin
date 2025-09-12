// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: JavaClass.java
public class JavaClass {
    public static MySealed foo() { return null; }
}

// FILE: main.kt
sealed interface MySealed {
    class Left(val x: String): MySealed
    class Right(val y: String): MySealed
}

interface MyGeneric<E1> {}

sealed interface MyGenericSealed<E2> : MyGeneric<E2> {
    class Left(val x: String): MyGenericSealed<String>
    class Right(val y: String): MyGenericSealed<String>
}

fun main(mg: MyGeneric<Any>, cs: CharSequence) {
    when (val foo = JavaClass.foo()) {
        is Left -> foo.x
        is Right -> foo.y
    }

    // mg: MyGenericSealed<String> & MyGeneric<Any> => MyGenericSealed <: MyGeneric => should work
    mg as MyGenericSealed<String>
    when (mg) {
        is Left -> mg.x
        is Right -> mg.y
    }

    // cs: MyGenericSealed<String> & CharSequence => neither component is a subclass of another => should not work
    cs as MyGenericSealed<String>
    when (cs) {
        is Left -> cs.x
        is Right -> cs.y
    }
}

// IGNORE_STABILITY_K1: candidates