// ISSUE: KT-65844

@RequiresOptIn
annotation class MyOptIn

@MyOptIn
fun foo() {}

@OptIn(markerClass = [MyOptIn::class]) // should be ok
class MyClass {
    fun test() {
        foo()
    }
}
