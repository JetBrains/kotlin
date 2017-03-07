val x = 42

fun main(args: Array<String>) {
    val p = ::x
    println(p.get())
}