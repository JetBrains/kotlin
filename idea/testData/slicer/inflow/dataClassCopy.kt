// FLOW: IN

data class DataClass(val value1: Int, val value2: Int)

fun foo(dataClass: DataClass) {
    val <caret>v = dataClass.value1
}

fun bar() {
    val dataClass = DataClass(1, 2)
    foo(dataClass)
    foo(dataClass.copy(value1 = 10))
    foo(dataClass.copy(value2 = 11))
    foo(DataClass(value1 = 1, value2 = 2))
}