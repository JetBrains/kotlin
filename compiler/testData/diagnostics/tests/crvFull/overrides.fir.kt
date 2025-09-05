// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib0
// FILE: Super.kt

interface Super {
    fun a(): Int // MustUse
}

// MODULE: lib1(lib0)
// FILE: Lib.kt

interface Base: Super {
    @IgnorableReturnValue override fun a(): Int
    fun b(): Int
}

class Impl: Base {
    override fun a(): Int = 42
    override fun b(): Int = 43
}

class MyException(override val message: String?): Throwable()

fun MyException(message: String?, cause: Throwable?): MyException {
    return MyException(message).also { it.initCause(cause) }
}

// MODULE: main(lib0, lib1)

// FILE: App.kt

fun main(a: Base, b: Impl, s: Super) {
    s.<!RETURN_VALUE_NOT_USED!>a<!>()
    a.a()
    b.a()
    a.<!RETURN_VALUE_NOT_USED!>b<!>()
    b.<!RETURN_VALUE_NOT_USED!>b<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, integerLiteral, interfaceDeclaration,
javaFunction, lambdaLiteral, nullableType, override, primaryConstructor, propertyDeclaration */
