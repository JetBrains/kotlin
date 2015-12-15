open class A {
    open var foo: Boolean = true
}

interface IA {
    var foo: String
}

interface IAA {
    var foo: Int
}

interface IGA<T> {
    var foo: T
}

<!VAR_TYPE_MISMATCH_ON_INHERITANCE!>class B1<!>: A(), IA

<!VAR_TYPE_MISMATCH_ON_INHERITANCE!>class B2<!>: A(), IA, IAA

abstract <!VAR_TYPE_MISMATCH_ON_INHERITANCE!>class B3<!>: IA, IAA

class BS1: A(), IGA<Boolean>

<!VAR_TYPE_MISMATCH_ON_INHERITANCE!>class BS2<!>: A(), IGA<Any>

<!VAR_TYPE_MISMATCH_ON_INHERITANCE!>class BS3<!>: A(), IGA<String>

<!VAR_TYPE_MISMATCH_ON_INHERITANCE!>class BG1<!><T>: A(), IGA<T>
