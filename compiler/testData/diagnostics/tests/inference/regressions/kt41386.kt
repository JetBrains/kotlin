// FIR_IDENTICAL
open class Test<T1, T2>(val map1 : Map<T1, T2>, val map2 : Map<T2, T1>) {
    open val inverse: Test<T2, T1> = object : Test<T2, T1>(map2, map1) {
        override val inverse: Test<T1, T2>
            get() = this@Test
    }
}
