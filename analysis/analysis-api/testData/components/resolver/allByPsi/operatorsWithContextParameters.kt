class A

context(a: A)
operator fun A.unaryPlus() {}

context(a: A)
operator fun A.plus(b: Int) {}

context(a: A)
operator fun A.get(b: Int) {}

context(a: A)
operator fun A.set(b: Int, x: A) {}

context(a: A)
operator fun A.invoke() {}

context(a: A)
operator fun A.plusAssign(b: Int) {}

context(a: A)
operator fun A.inc(): A { return this }

class SimpleOperators {
    context(a: A)
    operator fun unaryPlus() {}

    context(a: A)
    operator fun plus(b: Int) {}

    context(a: A)
    operator fun get(b: Int) {}

    context(a: A)
    operator fun set(b: Int, x: A) {}

    context(a: A)
    operator fun invoke() {}

    context(a: A)
    operator fun plusAssign(b: Int) {}

    context(a: A)
    operator fun inc(): SimpleOperators { return this@SimpleOperators }
}

fun usage() {
    var a = A()
    var b = SimpleOperators()
    with(A()){
        var thisProp = this
        +thisProp
        +a
        thisProp + 1
        a + 1
        thisProp[1]
        a[1]
        thisProp[1] = A()
        a[1] = A()
        thisProp()
        a()
        thisProp += 1
        a += 1
        thisProp++
        a = a++

        b + 1
        +b
        b[1]
        b[1] = A()
        b()
        b += 1
        b++
    }
}

fun withoutContext() {
    var a = A()
    var b = SimpleOperators()

    +a
    a + 1
    a[1]
    a[1] = A()
    a()
    a += 1
    a++

    b + 1
    +b
    b[1]
    b[1] = A()
    b()
    b += 1
    b++
}

// LANGUAGE: +ContextParameters
