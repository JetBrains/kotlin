operator fun Int?.inc(): Int? = this

fun init(): Int? { return 10 }

public fun box() : String {
    var i : Int? = init()
    val j = ++i

    return if (j == 10 && 10 == i) "OK" else "fail i = $i j = $j"
}
