// "Create parameter 'foo'" "true"

class A {
    fun test<T>(n: Int,
                foo: T) {
        val t: T = foo
    }
}
