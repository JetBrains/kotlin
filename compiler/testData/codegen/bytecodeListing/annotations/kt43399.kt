interface B {
    @A
    val Array<Int>.a: Int

    @A
    val Array<Array<Int>>.b: Int

    @A
    val Array<IntArray>.c: Int

    @A
    val Array<*>.d: Int

    @A
    val Array<out String>.e: Int

    @A
    val Array<in String>.f: Int
}

annotation class A