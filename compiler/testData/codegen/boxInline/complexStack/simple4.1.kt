import test.*

fun box(): String {
    return processRecords { a, b -> a + b}
}