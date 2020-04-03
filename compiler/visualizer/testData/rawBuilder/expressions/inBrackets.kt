// FIR_IGNORE
fun test(e: Int.() -> String) {
//      String
//      │   Int
//      │   │ fun Int.invoke(): String
//      │   │ │
    val s = 3.e()
//      String
//      │    Int
//      │    │ fun Int.invoke(): String
//      │    │ │test.e: Int.() -> String
//      │    │ ││
    val ss = 3.(e)()
}
