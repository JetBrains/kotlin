open class C {
    val x = 1
}

trait Tr : C {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}