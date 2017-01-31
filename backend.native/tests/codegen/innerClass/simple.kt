class Outer {
    inner class Inner {
        fun box() = "OK"
    }
}

fun box() = Outer().Inner().box()

fun main(args: Array<String>)
{
    println(box())
}