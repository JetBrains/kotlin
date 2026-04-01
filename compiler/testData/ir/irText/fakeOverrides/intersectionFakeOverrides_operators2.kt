// The test primarily tests reflect dumps (k1 vs new reflect), we don't need kt dumps
// SKIP_KT_DUMP

// FILE: main.kt
public open class B {
    fun plus(a: Any): String = ""
    fun infixFun(a: Any): String = ""
}

public interface D {
    operator fun plus(a: Any): Any
    infix fun infixFun(a: Any): Any
}

public interface C {
    fun plus(a: Any): CharSequence
    fun infixFun(a: Any): CharSequence
}

class A1 : B(), C, D

class A2 : B(), D, C

class A3 : B(), C

class A4 : B(), D