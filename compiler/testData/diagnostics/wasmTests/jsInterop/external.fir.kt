
// Classes

external class C1

external enum class C2

external annotation class C3

external data class C4(val x: String)

external class C5 {

    class C6

    inner class C7
}

external inline class C8(val x: Int)

external value class C9(val x: Int)


// Interfaces

external interface I1

external fun interface I2 {
    fun foo(): Int
}


// Functions

external fun foo1(): Int

external <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo2(): Int

external inline fun foo3(f: () -> Int): Int

external suspend fun foo4(): Int

external fun Int.foo5(): Int


// Properties

external lateinit var v1: String

external val Int.v2: String
    get() = definedExternally
