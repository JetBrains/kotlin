// LANGUAGE: +NameBasedDestructuring
package foo

data class MyDataClass(val prop1: Int, val prop2: String)

fun usage() {
    (val prop1, val <expr>prop2</expr>) = MyDataClass(1, "test")
}
