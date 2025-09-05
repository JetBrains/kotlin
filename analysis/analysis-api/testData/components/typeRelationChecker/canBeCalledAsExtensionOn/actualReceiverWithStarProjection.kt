interface Container<A>

fun Container<Int>.container<caret>Extension() {}

fun usage(x: Container<*>) {
    <expr>x</expr>
}