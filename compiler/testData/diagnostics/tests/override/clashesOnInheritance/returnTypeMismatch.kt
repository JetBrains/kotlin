open class A {
    open fun foo(): Boolean = true
}

interface IA {
    fun foo(): String
}

interface IAA {
    fun foo(): Int
}

interface IGA<T> {
    fun foo(): T
}

<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class B1<!>: A(), IA

<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class B2<!>: A(), IA, IAA

abstract <!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class B3<!>: IA, IAA

class BS1: A(), IGA<Boolean>

class BS2: A(), IGA<Any>

<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class BS3<!>: A(), IGA<String>

<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class BG1<!><T>: A(), IGA<T>
