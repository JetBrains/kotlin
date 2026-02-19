// LANGUAGE: +NameBasedDestructuring
package foo

data class MyDataClass<T>(val prop1: Int, val prop2: T)

fun usage() {
    (val prop1, <expr>val prop2</expr>) = MyDataClass(1, "test")
}
