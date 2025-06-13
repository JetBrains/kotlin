// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// MODULE: lib1

// FILE: Lib.kt

interface Base {
    @IgnorableReturnValue
    fun a(): Int
}

class Impl: Base {
    override fun a(): Int = 42
}

class MyException(override val message: String?): Throwable()

fun MyException(message: String?, cause: Throwable?): MyException {
    return MyException(message).also { it.initCause(cause) }
}

// MODULE: main(lib1)

// FILE: App.kt

fun main(a: Base, b: Impl) {
    a.a()
    <!RETURN_VALUE_NOT_USED!>b.a()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, integerLiteral, interfaceDeclaration,
javaFunction, lambdaLiteral, nullableType, override, primaryConstructor, propertyDeclaration */
