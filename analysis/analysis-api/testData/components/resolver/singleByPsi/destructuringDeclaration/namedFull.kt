// LANGUAGE: +NameBasedDestructuring
package foo

data class MyDataClass(val prop1: Int, val prop2: String)

fun usage() {
    (val prop1, <expr>val pro<caret>p2</expr>) = MyDataClass(1, "test")
}
