class A(
    val x: String,
    val y: String,
) {
    constructor(
        x: Comparable<Comparable<Number>>,
        y: Iterable<Iterable<Number>> ,
    ) {}

    var x: Int
        get() = 10
        set(value,) {

        }

    var x: Int
        get() = 10
        set(value,/*...*/) {

        }

    var x: Int
        get() = 10
        set(value/*...*/,) {

        }
}

fun foo(
    x: Int,
    y: Number
    ,
) {}

val foo: (Int, Int) -> Int = fun(x,
                                 y, ): Int {
    return x + y
}

val foo: (Int, Int, Int) -> Int =
    fun (x,
         y: Int, z,
         ): Int {
        return x + y
    }

fun foo() = listOf(
    foo.bar.something(),
    "foo bar something"
    ,
    )

fun foo() {
    val x = x[
            1,
            3
            , ]
    val y = x[
            1,
            3
            ,
    ]
}

fun main() {
    val x = {
            x: Comparable<Comparable<Number>>,
            y: Iterable<Iterable<Number>>
            ,->
        println("1")
    }
    val y = {
            x: Comparable<Comparable<Number>>,
            y: Iterable<Iterable<Number>>,
        -> println("1")
    }
    val z = {
            x: Comparable<Comparable<Number>>,
            y: Iterable<Iterable<Number>>
            ,
        ->
        println("1")
    }
}

fun foo(x: Any) = when (x) {
    Comparable::class,
    Iterable::class,
    String::class,
        -> println(1)
    else -> println(3)
}

fun foo(x: Any) = when (x) {
    Comparable::class,
    Iterable::class,
    String::class,->
        println(1)
    else -> println(3)
}

fun foo(x: Any) = when (x) {
    Comparable::class,
    Iterable::class,
    String::class
        ,
    ->
        println(1)
    else -> println(3)
}

@Anno([1, 2, 3, 4
          ,]
)
fun foo() {}

fun main() {
    val (
        y,
        z,
    ) = Pair(1, 2)
    val (
        y,
        z
            , ) = Pair(1, 2)
}

class A<
        T1: Number,
        T2: Iterable<Iterable<Iterable<Number>>>,
        T3: Comparable<Comparable<Comparable<Number>>>,
        > { }

fun <
        T1: Comparable<Comparable<Number>>,
        T2: Iterable<Iterable<Number>>
        ,
        > foo() {}

fun main() {
    foo<Comparable<Comparable<Number>>, Iterable<Iterable<Number>>,>()
}

fun main() {
    val x: (
        y: Comparable<Comparable<Number>>,
        z: Iterable<Iterable<Number>>,
        ) -> Int = { 10 }

    val y = foo(1,) {}

    try {
        println(1)
    } catch (e: Exception,) {

    }
}
