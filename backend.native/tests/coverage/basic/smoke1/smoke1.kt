package coverage.basic.smoke1

class A(val prop: Int) {

    constructor() : this(1)

    fun action1() {
        println(prop)
    }

    fun action2() {

    }
}

class B private constructor(val prop: String) {

    init {
        println("init block")
    }

    constructor(prop1: String, prop2: String) : this("$prop1, $prop2")

    constructor() : this("dummy") {
        if (1 > 2) {
            println("uncovered")
        } else {
            println("foo")
        }
        println("bar")
    }

    override fun toString() = prop
}

fun main(args: Array<String>) {
    val a1 = A(2)
    a1.action1()
    a1.action2()
    val a2 = A()
    a2.action1()
    a2.action1()

    val b1 = B("Hello", "world")
    val b2 = B()
    println(b1)
    println(b2)
}
