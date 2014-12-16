class A {
    fun foo() {
        println(this)
    }

    fun bar() {
        println(this)
    }
}

class B {
    fun foo() {
        println(this)
    }

    fun bar() {
        println(this)
    }
}

fun A.foo() {
    println(this)
}

fun A.bar() {
    println(this)
}

fun B.foo() {
    println(<selection>this</selection>)
}

fun B.bar() {
    println(this)
}