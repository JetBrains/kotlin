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
// 1 INVOKESTATIC java/lang/String.valueOf \(Z\)
// 3 INVOKESTATIC java/lang/String.valueOf \(I\)
// 1 INVOKESTATIC java/lang/String.valueOf \(C\)
// 1 INVOKESTATIC java/lang/String.valueOf \(J\)
// 1 INVOKESTATIC java/lang/String.valueOf \(F\)
// 1 INVOKESTATIC java/lang/String.valueOf \(D\)
// 8 valueOf
