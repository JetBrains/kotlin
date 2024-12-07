class Test {
    private fun test(x: Int, y: Int) {
        val (_, _) = invert(x, y)
    }

    private fun invert(x: Int, y: Int): Point {
        return Point(-x, -y)
    }
}

data class Point(val x: Int, val y: Int)