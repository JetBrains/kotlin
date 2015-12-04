open class A {
    open val foo: Boolean = true
}

interface IA {
    val foo: String
}

interface IAA {
    val foo: Int
}

interface IGA<T> {
    val foo: T
}

<!PROPERTY_TYPE_MISMATCH_ON_INHERITANCE!>class B1<!>: A(), IA

<!PROPERTY_TYPE_MISMATCH_ON_INHERITANCE!>class B2<!>: A(), IA, IAA

abstract <!PROPERTY_TYPE_MISMATCH_ON_INHERITANCE!>class B3<!>: IA, IAA

class BS1: A(), IGA<Boolean>

class BS2: A(), IGA<Any>

<!PROPERTY_TYPE_MISMATCH_ON_INHERITANCE!>class BS3<!>: A(), IGA<String>

<!PROPERTY_TYPE_MISMATCH_ON_INHERITANCE!>class BG1<!><T>: A(), IGA<T>
