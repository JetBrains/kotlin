// LANGUAGE: +StrictEquals

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
open class Base {
    val value: Int get() = 42
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = true
}

class Explicit : Base()

class Inherited : Base() {
    override fun equals(other: Any?): Boolean = true
}

data class Data(val x: Int)

interface Top

interface Left : Top {
    fun left()
    override fun equals(@EqualityBound(Left::class) other: Any?): Boolean
}

interface Right : Top {
    override fun equals(@EqualityBound(Top::class) other: Any?): Boolean
}

interface LeftAndRight : Left, Right

// MODULE: main(lib)
// FILE: main.kt
fun useSite(explicit: Explicit, inherited: Inherited, data: Data, any: Any?) {
    if (explicit == any) {
        any.value
    }

    if (inherited == any) {
        any.value
    }

    if (data == any) {
        any.x
    }
}

// `LeftAndRight.equals` is an intersection override of `Left.equals` and `Right.equals`.
// Its bound has to be the most specific one of the two, `Left`.
fun useIntersectionOverride(leftAndRight: LeftAndRight, any: Any?) {
    if (leftAndRight == any) {
        any.left()
    }
}
