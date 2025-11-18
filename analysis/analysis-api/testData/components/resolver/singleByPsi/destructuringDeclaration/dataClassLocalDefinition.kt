package foo

data class MyDataClass(val prop1: Int, val prop2: String)

fun usage() {
    val myDataClass = MyDataClass(1, "test")
    val (prop1, <expr>prop2</expr>) = myDataClass
}
