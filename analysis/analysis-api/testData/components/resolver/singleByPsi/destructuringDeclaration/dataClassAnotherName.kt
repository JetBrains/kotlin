package foo

data class MyDataClass(val prop1: Int, val prop2: String)

fun usage() {
    val (prop1, <expr>localProp2</expr>) = MyDataClass(1, "test")
}
