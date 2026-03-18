fun testTailrec(): String {
    val r1 = foo(5)
    val r2 = foo()
    return if (r1 == 1 && r2 == 1) "OK" else "FAIL:r1=$r1,r2=$r2"
}