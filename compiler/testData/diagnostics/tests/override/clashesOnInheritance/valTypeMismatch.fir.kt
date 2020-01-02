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

class B1: A(), IA

class B2: A(), IA, IAA

abstract class B3: IA, IAA

class BS1: A(), IGA<Boolean>

class BS2: A(), IGA<Any>

class BS3: A(), IGA<String>

class BG1<T>: A(), IGA<T>
