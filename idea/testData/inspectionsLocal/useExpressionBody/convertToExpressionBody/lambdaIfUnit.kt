// IS_APPLICABLE: true

fun <T> run(f: () -> T) = f()
fun whenExpr(flag: Boolean) {
    <caret>run {
        println("run")
        if (flag) {
            println("flag")
        }
    }
}
fun println(s: String) {}