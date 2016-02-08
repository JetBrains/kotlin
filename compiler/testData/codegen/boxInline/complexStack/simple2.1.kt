import test.*

fun box(): String {
    return processRecords { "O" + it }
}