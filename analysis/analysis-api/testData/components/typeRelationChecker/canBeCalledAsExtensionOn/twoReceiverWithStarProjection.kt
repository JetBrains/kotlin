interface Container<A>

fun Container<*>.container<caret>Extension() {}

fun usage(x: Container<*>) {
    <expr>x</expr>
}