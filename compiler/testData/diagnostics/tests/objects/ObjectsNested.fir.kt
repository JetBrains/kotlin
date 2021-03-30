package nestedObejcts

object A {
    val b = B
    val d = A.B.A

    object B {
        val a = A
        val e = B.A

        object A {
            val a = A
            val b = B
            val x = nestedObejcts.A.B.A
            val y = this@A
        }
    }

}
object B {
    val b = B
    val c = A.B
}

val a = A
val b = B
val c = A.B
val d = A.B.A
val e = B.<!UNRESOLVED_REFERENCE!>A<!>.B
