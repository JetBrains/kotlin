// "Create property 'foo'" "true"
// ERROR: Property must be initialized or be abstract
class A {
    private var foo: Int

    fun test() {
        foo = 1
    }
}