// !WITH_NEW_INFERENCE
// !LANGUAGE: -ShortSyntaxForPropertyGetters
// NI_EXPECTED_FILE

// blockBodyGetter.kt
val x get() {
    return 1
}

// cantBeInferred.kt
val x1 get() = foo()
val y1 get() = bar()

fun <E> foo(): E = null!!
fun <E> bar(): List<E> = null!!

// explicitGetterType.kt
val x2 get(): String = foo()
val y2 get(): List<Int> = bar()
val z2 get(): List<Int> {
    return bar()
}

val u get(): String = <!UNRESOLVED_REFERENCE!>field<!>

// members.kt
class A {
    val x get() = 1
    val y get() = id(1)
    val y2 get() = id(id(2))
    val z get() = l("")
    val z2 get() = l(id(l("")))

    val <T> T.u get() = id(this)
}
fun <E> id(x: E) = x
fun <E> l(x: E): List<E> = null!!

// vars
var x3
    get() = 1
    set(q) {
    }

// recursive
val x4 get() = x4

// null as nothing
val x5 get() = null
val y5 get() = null!!

// objectExpression.kt
object Outer {
    private var x
        get() = object : CharSequence {
            override val length: Int
                get() = 0

            override fun get(index: Int): Char {
                return ' '
            }

            override fun subSequence(startIndex: Int, endIndex: Int) = ""

            fun bar() {
            }
        }
        set(q) {
            x = q
        }
}
