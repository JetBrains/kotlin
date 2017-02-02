fun main(args: Array<String>) {
    var cnt = 0

    var x: Any = ""

    for (i in 0 .. 1) {
        print(x)
        cnt++
        val y = object {
            override fun toString() = cnt.toString()
        }
        x = y
    }
    print(x)
}

fun print(x: Any) = println(x.toString())