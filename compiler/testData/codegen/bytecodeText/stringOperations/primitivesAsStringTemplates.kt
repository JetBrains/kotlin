fun test(a: Boolean, b: Byte, c: Char, s: Short, i: Int, l: Long, f: Float, d: Double) {
    "$a"
    "$b"
    "$c"
    "$s"
    "$i"
    "$l"
    "$f"
    "$d"
}

// 1 INVOKESTATIC java/lang/String.valueOf \(Z\)
// 3 INVOKESTATIC java/lang/String.valueOf \(I\)
// 1 INVOKESTATIC java/lang/String.valueOf \(C\)
// 1 INVOKESTATIC java/lang/String.valueOf \(J\)
// 1 INVOKESTATIC java/lang/String.valueOf \(F\)
// 1 INVOKESTATIC java/lang/String.valueOf \(D\)
// 8 valueOf
