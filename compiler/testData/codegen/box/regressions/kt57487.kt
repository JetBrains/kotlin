// Minimized from libraries.stdlib.test.collections.ArraysTest.sortedTests()
// Exception is: Class found but error nodes are not allowed
fun <T: Comparable<T>> arrayData(vararg values: T, toArray: Array<T>.() -> Unit) {}
fun box(): String {
    arrayData(42) { }
    return "OK"
}
