// See KT-10824: Smart cast depending on control flow does not work inside `if`
class A
fun foo(a: A?, aOther: A?): A {
    return if (a == null) {
        A()
    }
    else {
        var newA = aOther
        if (newA == null) {
            newA = A()
        }
        newA
    }
}
fun bar(a: A?, aOther: A?): A {
    return if (a == null) {
        A()
    }
    else {
        if (aOther == null) {
            return A()
        }

        aOther
    }
}
fun foo1(a: A?, aOther: A?): A {
    val result = if (a == null) {
        A()
    }
    else {
        var newA = aOther
        if (newA == null) {
            newA = A()
        }
        newA
    }
    return result
}
fun bar1(a: A?, aOther: A?): A {
    val result = if (a == null) {
        A()
    }
    else {
        if (aOther == null) {
            return A()
        }

        aOther
    }
    return result
}