// LANGUAGE: +FullValueClasses
value class Point(val x: Int, val y: Int) {
    val sum: Int
        get() {
            return <expr>x</expr> + y
        }
}
