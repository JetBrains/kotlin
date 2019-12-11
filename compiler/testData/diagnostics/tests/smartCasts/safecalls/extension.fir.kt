fun String.foo(arg: Int) = this[arg]

fun calc(x: String?) {
    // x should be non-null in arguments list
    x?.foo(x.length - 1)
}
