external annotation class A(val x: Int)

val x: Int
    external get() = definedExternally

class B

val B.x: Int
    external get() = definedExternally

class C {
    val a: Int
        external get() = definedExternally
}

external class D {
    val a: Int
        external get() = definedExternally
}

external data class E(val x: Int)
