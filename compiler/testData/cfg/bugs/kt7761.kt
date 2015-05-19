class UnitIncDec() {
    fun inc() : Unit {}
    fun dec() : Unit {}
}

fun testUnitIncDec() {
    var x = UnitIncDec()
    x = <warning>x<error>++</error></warning>
}