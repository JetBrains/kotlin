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

class B1: A(), IA

class B2: A(), IA, IAA

abstract class B3: IA, IAA

class BS1: A(), IGA<Boolean>

class BS2: A(), IGA<Any>

class BS3: A(), IGA<String>

class BG1<T>: A(), IGA<T>
