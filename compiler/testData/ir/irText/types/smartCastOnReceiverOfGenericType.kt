// FIR_IDENTICAL

fun testFunction(a: Any, b: Any) {
    a as MutableList<String>
    b as String
    a.add(b)
}

fun testProperty(a: Any, b: Any) {
    a as Cell<String>
    b as String
    a.value = b
}

fun testInnerClass(a: Any, b: Any, c: Any) {
    a as Outer<Int>.Inner<String>
    b as Int
    c as String
    a.use(b, c)
}

fun <T> testNonSubstitutedTypeParameter(a: Any, b: Any) {
    a as MutableList<List<T>>
    b as List<T>
    a.add(b)
}

class Cell<T>(var value: T)

class Outer<T1> {
    inner class Inner<T2> {
        fun use(x1: T1, x2: T2) {}
    }
}

