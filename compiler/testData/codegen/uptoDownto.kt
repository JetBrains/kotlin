fun box(): String {
    val sb = StringBuilder()

    fun ap(i : Int) {
        if (sb.size != 0) sb.append(" ")
        sb.append(i)
    }

    for (i in 0..0) {
        ap(i)
    }
    sb.append(";")

    for (i in 0..1) {
        ap(i)
    }
    sb.append(";")

    for (i in IntRange(0, 1)) {
        ap(i)
    }
    sb.append(";")

    for (i in 0 downto 0) {
        ap(i)
    }
    sb.append(";")

    for (i in 1 downto 0) {
        ap(i)
    }

    return if (sb.toString() == "0; 0 1; 0; 0; 1 0") "OK" else sb.toString()!!
}

fun main(args: Array<String>) {
   println(box())
}