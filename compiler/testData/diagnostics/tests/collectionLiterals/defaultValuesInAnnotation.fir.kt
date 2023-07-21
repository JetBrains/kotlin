import kotlin.reflect.KClass

annotation class Foo(
        val a: Array<String> = ["/"],
        val b: Array<String> = [],
        val c: Array<String> = ["1", "2"],
        val d: Array<KClass<*>> = [Int::class, Array<Int>::class],
)

annotation class Bar(
        val a: Array<String> = <!TYPE_MISMATCH, TYPE_MISMATCH!>[' ']<!>,
        val b: Array<String> = ["", <!EMPTY_CHARACTER_LITERAL!>''<!>],
        val c: Array<String> = <!TYPE_MISMATCH, TYPE_MISMATCH!>[1]<!>
)

annotation class Base(
        val a0: IntArray = [],
        val a1: IntArray = [1],
        val b1: FloatArray = [1f],
        val b0: FloatArray = []
)

annotation class Err(
        val a: IntArray = [<!ARGUMENT_TYPE_MISMATCH!>1L<!>],
        val b: Array<String> = <!TYPE_MISMATCH, TYPE_MISMATCH!>[1]<!>
)
