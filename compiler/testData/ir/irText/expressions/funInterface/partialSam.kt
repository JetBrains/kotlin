// WITH_STDLIB

fun interface Fn<T, R> {
    fun run(s: String, i: Int, t: T): R
}

class J {
    fun runConversion(f1: Fn<String, Int>, f2: Fn<Int, String>): Int {
        return f1.run("Bar", 1, f2.run("Foo", 42, 239))
    }
}

val fsi = object : Fn<String, Int> {
    override fun run(s: String, i: Int, t: String): Int = 1
}

val fis = object : Fn<Int, String> {
    override fun run(s: String, i: Int, t: Int): String = ""
}

fun test(j: J) {
    j.runConversion(fsi) { s, i, ti -> ""}
    j.runConversion({ s, i, ts -> 1 }, fis)
}
