package aa

val a : Int = b
val b : Int = a + b

class C {
    val a : Int = b
    val b : Int = a + b
}

fun foo() {
    val a : Int
    a + 1
    a + 1
}
