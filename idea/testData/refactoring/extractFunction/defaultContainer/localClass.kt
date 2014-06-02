class A {
    fun foo(a: Int, b: Int): Int {
        class L: Function0<Int> {
            override fun invoke(): Int {
                return <selection>a + b - 1</selection>
            }
        }

        return L().invoke()
    }
}