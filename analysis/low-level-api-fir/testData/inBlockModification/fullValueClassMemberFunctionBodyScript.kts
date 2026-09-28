// LANGUAGE: +FullValueClasses
value class Point(val x: Int, val y: Int) {
    fun sum(): Int {
        return <expr>plus</expr>(x, y)
    }
}

fun plus(a: Int, b: Int): Int = a + b
