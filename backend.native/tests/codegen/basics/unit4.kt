fun main(args: Array<String>) {
    for (x in 0 .. 8) {
        foo(x, Unit)
    }
    println("Done")
}

var global = 42

fun foo(x: Int, unit: Unit) {
    var local = 5
    val y: Unit = when (x) {
        0 -> {}
        1 -> local = 6
        2 -> global = 43
        3 -> unit
        4 -> Unit
        5 -> bar()
        6 -> return
        7 -> {
            5
            bar()
        }
        8 -> {
            val z: Any = Unit
            z as Unit
        }
        else -> throw Error()
    }

    if (y !== Unit) {
        println("Fail at x = $x")
    }
}

fun bar() {
}