// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
package foo

data class MyDataClass(val prop1: Int, val prop2: String)

fun usage() {
    (val prop1, <expr>val prop2</expr>) = MyDataClass(1, "test")
}
