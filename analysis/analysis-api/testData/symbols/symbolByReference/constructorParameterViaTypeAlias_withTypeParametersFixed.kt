// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
package test

class MyClass<T>(param: T)

typealias MyAlias = MyClass<String>

fun usage() {
    MyAlias(<caret>param = "hello")
}