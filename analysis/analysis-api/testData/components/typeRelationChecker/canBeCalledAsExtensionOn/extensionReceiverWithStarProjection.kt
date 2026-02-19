interface Container<A>

fun Container<*>.container<caret>Extension() {}

fun usage(x: Container<String>) {
    <expr>x</expr>
}