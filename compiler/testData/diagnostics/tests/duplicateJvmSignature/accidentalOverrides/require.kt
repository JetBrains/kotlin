open class C {
    val x = 1
}

trait Tr : <!TRAIT_WITH_SUPERCLASS!>C<!> {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}