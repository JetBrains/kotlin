// FIR_IDENTICAL

// K1 and new kotlin reflect mismatch in:
// 1. isInline/isExternal. Compiler bug: KT-82842. New reflect is correct
// 2. Minor flexible types mismatches
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: JavaIntermediate.java
public class JavaIntermediate extends A<String> {
    public native void javaNativeMethod();
}

// FILE: main.kt
open class A<T> {
    external fun externalFun(): T
    external fun externalFunSpecialized(): String

    inline fun inlineFun(): T = null!!
    inline fun inlineFunSpecialized(): String = null!!

    infix fun infixFun(a: Any): T = null!!
    infix fun infixFunSpecialized(a: Any): String = null!!

    operator fun plus(a: Any): T = null!!
    operator fun minus(a: Any): String = null!!
}

open class KotlinIntermediate : A<String>()

interface I {
    fun externalFun(): String
    fun externalFunSpecialized(): String

    fun inlineFun(): String
    fun inlineFunSpecialized(): String

    fun infixFun(a: Any): String
    fun infixFunSpecialized(a: Any): String

    operator fun plus(a: Any): String
    operator fun minus(a: Any): String
}

class C1 : JavaIntermediate(), I
class C2 : KotlinIntermediate(), I

class D : A<String>(), I

class E1 : JavaIntermediate()
class E2 : KotlinIntermediate()
