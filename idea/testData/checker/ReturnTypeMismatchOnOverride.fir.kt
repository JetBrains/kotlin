interface Base {
    fun foo(): Int
    var bar: Int
    val qux: Int
}

class Derived : Base {
    override fun foo(): <error descr="[RETURN_TYPE_MISMATCH_ON_OVERRIDE] Return type of 'foo' is not a subtype of the return type of the overridden member 'foo'">String</error> = ""
    override var bar: <error descr="[VAR_TYPE_MISMATCH_ON_OVERRIDE] Type of 'bar' doesn't match the type of the overridden var-property 'bar'">String</error> = ""
    override val qux: <error descr="[PROPERTY_TYPE_MISMATCH_ON_OVERRIDE] Type of 'qux' is not a subtype of the overridden property 'qux'">String</error> = ""
}
