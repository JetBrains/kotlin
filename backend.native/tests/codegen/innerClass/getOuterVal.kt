class Outer(val s: String) {
    inner class Inner {
        fun box() = s
    }
}

fun box() = Outer("OK").Inner().box()

fun main(args: Array<String>)
{
    println(box())
}