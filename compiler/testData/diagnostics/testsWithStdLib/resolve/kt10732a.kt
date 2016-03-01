fun <T> List<T>?.foo() {}

// NB Not a redeclaration
@JvmName("f1")
fun <T> List<T>.foo() {}
