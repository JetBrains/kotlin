// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun foo() {
    val a = true
    val b = false
    fun test(a: Boolean): Boolean { return false }
    if (test(a &<caret>& b)) {
        doSomething("test")
    }
}
