// DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// LANGUAGE: -TrailingCommas

open class Foo1(x: Int = 10, y: Float = 0f,)

class Foo2(
    val x: Int = 10,
    var y: Float,
): Foo1(x, y,) {
    constructor(
        x: Float,
        y: Int = 10,
    ): this(1, 1f,) {}

    var x1: Int
        get() = 10
        set(value,) {

        }

    var x2: Int
        get() = 10
        set(
            x2,
        ) {}

    var x3: (Int) -> Unit
        get() = {}
        set(x2: (Int) -> Unit,) {}

    var x4: (Int) -> Unit
        get() = {}
        set(x2: (Int) -> Unit,/**/) {}
}

enum class Foo3(x: Int, )

fun foo4(x: Int, y: Comparable<Float>,) {}

fun foo5(x: Int = 10,) {}

fun foo6(vararg x: Int,) {}

fun foo7(y: Float, vararg x: Int,) {}

val foo8: (Int, Int,) -> Int = fun(
    x,
    y,
    ): Int {
    return x + y
}

val foo9: (Int, Int, Int,) -> Int =
    fun (x, y: Int, z,): Int {
        return x + y
    }

open class Foo10(x: Int = 10, y: Float = 0f)

class Foo11: Foo10 {
    constructor(
        x: Float
    ): super(1, 1f,)
}

class Foo12: Foo10 {
    constructor(
        x: Float
    ): super(1, 1f,/**/)
}

fun main() {
    val x1 = {
            x: Comparable<Comparable<Number>>,
            y: Iterable<Iterable<Number>>,
        ->
        println("1")
    }
    val x2 = { x: Comparable<Comparable<Number>>,
        -> println("1")
    }
    val x3: ((Int,) -> Int) -> Unit = { x: (Int,) -> Int, -> println("1") }
    val x4: ((Int,) -> Int) -> Unit = { x, -> println("1") }

    fun foo10(x:Int,y:Int,) {
        fun foo10(x:Int,y:Int,) {}
    }
    fun foo101(x:Int,y:Int,/**/) {
        fun foo10(x:Int,y:Int,/**/) {}
    }

    try {
        println(1)
    } catch (e: Exception,) {

    }

    try {
        println(1)
    } catch (e: Exception,) {

    } catch (e: Exception,) {

    }

    try {
        println(1)
    } catch (e: Exception,) {

    } finally {

    }

    try {
        println(1)
    } catch (e: Exception,/**/) {

    } finally {

    }
}