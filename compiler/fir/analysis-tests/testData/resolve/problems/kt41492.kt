// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-41492
// WITH_STDLIB
// FILE: JBase.java

public interface JBase {
    String getFoo();
}

// FILE: kt41492.kt

// KT-41492: Overload resolution ambiguity on intersection type with synthetic property

interface A : C, D
interface B : C, D

interface C : JBase
interface D : JBase

fun <K> select(x: K, y: K): K = TODO()

fun test(a: A, b: B) {
    val r = select(a, b)
    r.foo
}

fun test2(a: A, b: B) {
    val r = listOf(select(a, b))
    r.joinToString { it.foo }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, interfaceDeclaration, intersectionType, javaProperty, javaType,
localProperty, nullableType, propertyDeclaration, typeParameter */
