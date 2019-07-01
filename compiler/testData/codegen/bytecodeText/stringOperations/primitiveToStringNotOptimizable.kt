fun main(a: Boolean, b:Byte, c: Short, d: Int, e: Long, f: Float, g: Double, h: Char) {
    a.toString()
    b.toString()
    c.toString()
    d.toString()
    e.toString()
    f.toString()
    g.toString()
    h.toString()
}

/*Check that all "valueOf" are String ones and there is no boxing*/
// 8 valueOf
// 8 INVOKESTATIC java/lang/String.valueOf
