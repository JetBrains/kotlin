val flag = true

val a /*: (Int) -> String */ = l@ {
    i: Int ->
    if (i == 0) return@l i.toString()

    "Ok"
}

fun <T> foo(f: (Int) -> T): T = f(0)

val b /*:String */ = foo {
    i ->
    if (i == 0) return@foo i.toString()

    "Ok"
}