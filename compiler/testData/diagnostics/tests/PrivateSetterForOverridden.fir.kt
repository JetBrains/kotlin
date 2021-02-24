// See KT-10325: private setters are allowed for overridden properties in final class

interface A {
    val a: Int

    var b: Int
}

abstract class AA {
    abstract val c: Int

    abstract var d: Int
}

class B : A, AA() {
    override var a: Int = 0
        // Ok
        private set

    override var b: Int = 1
        private set

    override var c: Int = 2
        // Ok
        private set

    override var d: Int = 3
        private set
}

open class C : A, AA() {
    override var a: Int = 0
        // Errors here and below
        private set

    override var b: Int = 1
        private set

    override var c: Int = 2
        private set

    override var d: Int = 3
        private set
}

abstract class D : A, AA() {
    override var a: Int = 0
        // Errors here and below
        private set

    override var b: Int = 1
        private set

    override var c: Int = 2
        private set

    override var d: Int = 3
        private set
}

interface E : A {
    override var a: Int
        get() = 0
        // Errors here and below
        <!PRIVATE_SETTER_FOR_OPEN_PROPERTY!>private set(arg) {}<!>

    override var b: Int
        get() = 0
        <!PRIVATE_SETTER_FOR_OPEN_PROPERTY!>private set(arg) {}<!>
}