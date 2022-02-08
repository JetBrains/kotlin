fun use(i: Int) {}

fun testPostfixIncr0() {
    var k = 0
    k++
    use(k)
}

fun testPostfixIncr1() {
    var k = 0
    val t = k++
    use(k)
    use(t)
}

fun testPostfixDecr0() {
    var k = 0
    k--
    use(k)
}

fun testPostfixDecr1() {
    var k = 0
    val t = k--
    use(k)
    use(t)
}

// 6 ISTORE
// 8 ILOAD
// 0 ICONST_1
// 4 IINC