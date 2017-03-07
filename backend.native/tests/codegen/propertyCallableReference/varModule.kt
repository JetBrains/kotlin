var x = 42

fun main(args: Array<String>) {
    val p = ::x
    p.set(117)
    println(x)
    println(p.get())
}