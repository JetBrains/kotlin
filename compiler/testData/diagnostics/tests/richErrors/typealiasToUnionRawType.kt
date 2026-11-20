// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
// FILE: K.kt
error class E

typealias ListRes<T> = List<T> | E

open class K<T> {
    fun g(x: ListRes<T>): ListRes<T> = x
}

// FILE: J.java
public class J<T> extends K<T> {
    public static J raw() { return null; }
}

// FILE: test.kt
fun test(ls: List<String>) {
    val g = J.raw().g(ls)
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
