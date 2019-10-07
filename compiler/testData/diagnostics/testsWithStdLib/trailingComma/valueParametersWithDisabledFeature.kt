// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE

open class Foo1(x: Int = 10, y: Float = 0f<!UNSUPPORTED_FEATURE!>,<!>)

class Foo2(
    val x: Int = 10,
    var y: Float<!UNSUPPORTED_FEATURE!>,<!>
): Foo1(x, y<!UNSUPPORTED_FEATURE!>,<!>) {
    constructor(
        x: Float,
        y: Int = 10<!UNSUPPORTED_FEATURE!>,<!>
    ): this(1, 1f<!UNSUPPORTED_FEATURE!>,<!>) {}

    var x1: Int
        get() = 10
        set(value<!UNSUPPORTED_FEATURE!>,<!>) {

        }

    var x2: Int
        get() = 10
        set(
            x2<!UNSUPPORTED_FEATURE!>,<!>
        ) {}

    var x3: (Int) -> Unit
        get() = {}
        set(x2: (Int) -> Unit<!UNSUPPORTED_FEATURE!>,<!>) {}

    var x4: (Int) -> Unit
        get() = {}
        set(x2: (Int) -> Unit<!UNSUPPORTED_FEATURE!>,<!>/**/) {}
}

enum class Foo3(x: Int<!UNSUPPORTED_FEATURE!>,<!> )

fun foo4(x: Int, y: Comparable<Float><!UNSUPPORTED_FEATURE!>,<!>) {}

fun foo5(x: Int = 10<!UNSUPPORTED_FEATURE!>,<!>) {}

fun foo6(vararg x: Int<!UNSUPPORTED_FEATURE!>,<!>) {}

fun foo7(y: Float, vararg x: Int<!UNSUPPORTED_FEATURE!>,<!>) {}

val foo8: (Int, Int<!UNSUPPORTED_FEATURE!>,<!>) -> Int = fun(
    x,
    y<!UNSUPPORTED_FEATURE!>,<!>
    ): Int {
    return x + y
}

val foo9: (Int, Int, Int<!UNSUPPORTED_FEATURE!>,<!>) -> Int =
    fun (x, y: Int, z<!UNSUPPORTED_FEATURE!>,<!>): Int {
        return x + y
    }

open class Foo10(x: Int = 10, y: Float = 0f)

class Foo11: Foo10 {
    constructor(
        x: Float
    ): super(1, 1f<!UNSUPPORTED_FEATURE!>,<!>)
}

class Foo12: Foo10 {
    constructor(
        x: Float
    ): super(1, 1f<!UNSUPPORTED_FEATURE!>,<!>/**/)
}

fun main() {
    val x1 = {
            x: Comparable<Comparable<Number>>,
            y: Iterable<Iterable<Number>><!UNSUPPORTED_FEATURE!>,<!>
        ->
        println("1")
    }
    val x2 = { x: Comparable<Comparable<Number>><!UNSUPPORTED_FEATURE!>,<!>
        -> println("1")
    }
    val x3: ((Int<!UNSUPPORTED_FEATURE!>,<!>) -> Int) -> Unit = { x: (Int<!UNSUPPORTED_FEATURE!>,<!>) -> Int<!UNSUPPORTED_FEATURE!>,<!> -> println("1") }
    val x4: ((Int<!UNSUPPORTED_FEATURE!>,<!>) -> Int) -> Unit = { x<!UNSUPPORTED_FEATURE!>,<!> -> println("1") }

    fun foo10(x:Int,y:Int<!UNSUPPORTED_FEATURE!>,<!>) {
        fun foo10(x:Int,y:Int<!UNSUPPORTED_FEATURE!>,<!>) {}
    }
    fun foo101(x:Int,y:Int<!UNSUPPORTED_FEATURE!>,<!>/**/) {
        fun foo10(x:Int,y:Int<!UNSUPPORTED_FEATURE!>,<!>/**/) {}
    }

    try {
        println(1)
    } catch (e: Exception<!UNSUPPORTED_FEATURE!>,<!>) {

    }

    try {
        println(1)
    } catch (e: Exception<!UNSUPPORTED_FEATURE!>,<!>) {

    } catch (e: Exception<!UNSUPPORTED_FEATURE!>,<!>) {

    }

    try {
        println(1)
    } catch (e: Exception<!UNSUPPORTED_FEATURE!>,<!>) {

    } finally {

    }

    try {
        println(1)
    } catch (e: Exception<!UNSUPPORTED_FEATURE!>,<!>/**/) {

    } finally {

    }
}