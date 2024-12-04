fun test(f: (Int, Int) -> Array<Int>) =
    f('O'.toInt(), 'K'.toInt())

fun box(): String {
    val t = test(::arrayOf)
    return "${t[0].toChar()}${t[1].toChar()}"
}
