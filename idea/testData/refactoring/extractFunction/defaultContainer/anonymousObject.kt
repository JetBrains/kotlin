class A {
    fun foo(a: Int, b: Int): Int {
        return object: Function0<Int> {
            override fun invoke(): Int {
                return <selection>a + b - 1</selection>
            }
        }
    }
}