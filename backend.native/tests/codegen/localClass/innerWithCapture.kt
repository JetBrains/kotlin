fun box(s: String): String {
    class Local {
        open inner class Inner() {
            open fun result() = s
        }
    }

    return Local().Inner().result()
}

fun main(args : Array<String>) {
    println(box("OK"))
}