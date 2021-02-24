// "Add '@JvmName' annotation" "true"
// WITH_RUNTIME
interface Foo<T>

fun Foo<Int>.foo() = this

fun <caret>Foo<String>.foo() = this