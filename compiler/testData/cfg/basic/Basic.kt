fun f(a : Boolean) : Unit {
    1
    a
    2.toLong()
    foo(a, 3)
    genfun<Any>()
    flfun {1}
    3.equals(4)

    3 equals 4

    1 + 2

    a && true
    a || false

}

fun foo(a : Boolean, b : Int) : Unit {}

fun <T> genfun() : Unit {}

fun flfun(f : () -> Any) : Unit {}