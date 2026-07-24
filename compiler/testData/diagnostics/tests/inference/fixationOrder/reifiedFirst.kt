// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// ISSUE: KT-86728

// FILE: JavaId.java
public class JavaId {
    public static <I> I id(I i) { return i; }
}

// FILE: main.kt

inline fun <reified T1 : Any> decode(): T1? {
    return null
}

fun <T2 : Any> decodeNonReified(): T2? {
    return null
}

fun <R> myRun(x: () -> R): R = TODO()

sealed interface Base
sealed interface Derived : Base

val d: Derived = TODO()
val dFlexible = JavaId.id(d)

fun main() {
    val x1: Base = decode() ?: d
    val x2: Base = decodeNonReified() ?: d
    val x3: Base = myRun { decode() ?: d }
    val x4: Base = myRun { decodeNonReified() ?: d }

    val y1: Base = decode() ?: dFlexible
    val y2: Base = decodeNonReified() ?: dFlexible
    val y3: Base = myRun { decode() ?: dFlexible }
    val y4: Base = myRun { decodeNonReified() ?: dFlexible }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, inline, interfaceDeclaration, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, reified, sealed, typeConstraint, typeParameter */
