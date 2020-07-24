open class C {
    val x = 1
}

interface Tr : <!INTERFACE_WITH_SUPERCLASS!>C<!> {
    fun getX() = 1
}