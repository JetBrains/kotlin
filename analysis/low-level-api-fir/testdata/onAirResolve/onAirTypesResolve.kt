open class X {
    /*PLACE*/class PLACE
}

/*ONAIR*/class Y {
    open class BASE : X()
    class DERIVED : BASE()
    fun withType(arg: BASE) { }
}