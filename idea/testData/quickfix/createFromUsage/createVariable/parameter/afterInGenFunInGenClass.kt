// "Create parameter 'foo'" "true"

class A<T> {
    fun test<T>(n: Int,
                foo: T) {
        val t: T = foo
    }
}
