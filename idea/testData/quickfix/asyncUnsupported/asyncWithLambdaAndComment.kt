// "Migrate unsupported async syntax" "true"
fun async(f: () -> Unit) {}

fun test() {
    async<caret> /**/ {  }
}
