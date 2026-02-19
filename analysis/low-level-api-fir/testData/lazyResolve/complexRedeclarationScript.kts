// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
sealed class A

class B : A()

interface C : A

interface D : C, A

class E : B, A()

sealed class P {
    object H: P()
    class J : P()

    object T {
        object V : P()
        class M : P()
    }

    val p: P = object : P() {

    }

    val r = object : P() {

    }
}

class K : P()

object B {
    class <caret>I : P()
}

fun test() {
    class L : P()
    val a = object : P() {

    }
}
