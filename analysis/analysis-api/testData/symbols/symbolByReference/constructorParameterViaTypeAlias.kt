// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
package test

class MyClass(param: String)

typealias MyAlias = MyClass

fun usage() {
    MyAlias(<caret>param = "hello")
}