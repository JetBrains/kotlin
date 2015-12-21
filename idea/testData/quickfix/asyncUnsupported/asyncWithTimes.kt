// "Migrate unsupported async syntax" "true"
object async {
    operator fun times(f: () -> Unit) = f()
}

fun test() {
    asy<caret>nc* {  }
}
