// "Add '@JvmName' annotation" "true"
// WITH_RUNTIME
interface Bar<T, U>

fun Bar<Int, Double>.bar() = this

fun <caret>Bar<Int, Bar<Long, Bar<Double, String>>>.bar() = this