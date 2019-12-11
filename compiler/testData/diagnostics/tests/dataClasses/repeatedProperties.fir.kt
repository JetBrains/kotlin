data class A1(val x: Int, val y: String, val x: Int) {
    val z = ""
}

data class A2(val x: Int, val y: String) {
    val x = ""
}

data class A3(val<!SYNTAX!><!> :Int, val<!SYNTAX!><!> : Int)
