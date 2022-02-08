// FIR_IGNORE
fun test(e: Int.() -> String) {
//      String
//      │   Int
//      │   │ fun P1.invoke(): R
//      │   │ │
    val s = 3.e()
//      String
//      │    Int
//      │    │ fun P1.invoke(): R
//      │    │ │test.e: Int.() -> String
//      │    │ ││
    val ss = 3.(e)()
}
