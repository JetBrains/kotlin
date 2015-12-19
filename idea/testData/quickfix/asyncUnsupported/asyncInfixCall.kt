// "Migrate unsupported async syntax" "true"
infix fun Any.async(f: () -> Unit) = f()

fun test(foo: Any) {
    foo <caret>async { }
}