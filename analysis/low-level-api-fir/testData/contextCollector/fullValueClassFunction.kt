// LANGUAGE: +FullValueClasses
value class Point(val x: Int, val y: Int) {
    <expr>fun sum(): Int { return plus(x, y) }</expr>
    fun plus(a: Int, b: Int): Int = a + b
}
