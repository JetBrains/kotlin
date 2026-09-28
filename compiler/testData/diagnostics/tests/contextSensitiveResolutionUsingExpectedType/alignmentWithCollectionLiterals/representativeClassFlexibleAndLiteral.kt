// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89232
// FIR_DUMP
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

// FILE: JavaUtils.java

import java.util.List;
import java.util.Set;

public class JavaUtils {
    public static void takeEnum(MyEnum e) {}
    public static void takeList(List<MyEnum> l) {}
    public static void takeSet(Set<String> s) {}
}

// FILE: test.kt

enum class MyEnum { X, Y }

fun testFlexible() {
    JavaUtils.takeEnum(X)
    JavaUtils.takeList([X, Y])
    JavaUtils.takeSet(["a"])
    JavaUtils.takeSet([])
}

fun testIntegerLiteral(): Int {
    return when (10000000000) {
        MAX_VALUE -> 1
        else -> 0
    }
}

fun testIntegerLiteralAmbiguousType(): Int {
    return when (1) {
        MAX_VALUE -> 1
        else -> 0
    }
}

fun <T> select(x: T, y: T): T = x

fun testIntegerLiteralInSelect() {
    select(10000000000, MAX_VALUE)
    select(0, <!UNRESOLVED_REFERENCE!>MAX_VALUE<!>)
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, flexibleType, functionDeclaration, integerLiteral,
javaFunction, nullableType, stringLiteral, typeParameter, whenExpression, whenWithSubject */
