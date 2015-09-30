// FILE: B.kt

import aa.A.use
import aa.A.useList

fun testPrimitives(b: Byte, ss: Short, i: Int, l: Long, d: Double, s: String, f: Float, bool: Boolean) {
    use(b)
    use(ss)
    use(i)
    use(l)
    use(s)
    use(f)
    use(d)
    use(bool)
}

class N
class S: java.io.Serializable

fun testArrays(ia: IntArray, ai: Array<Int>, an: Array<N>, a: Array<S>) {
    use(ia)
    use(ai)
    use(an)
    use(a)
}

fun testLiterals() {
    use(1)
    use(1.0)
    use(11111111111111)
    use("Asdsd")
    use(true)
}

fun testNotSerializable(l: List<Int>) {
    use(<!TYPE_MISMATCH!>l<!>)
    use(<!TYPE_MISMATCH!>N()<!>)
}

enum class C {
    E, E2
}

fun testEnums(a: Enum<*>) {
    use(C.E)
    use(C.E2)
    use(a)
}

fun testLists(a: List<Int>) {
    useList(a)
}

// FILE: aa/A.java
package aa;

public class A {
    public static void use(java.io.Serializable s) { }
    public static void useList(java.util.List<? extends java.io.Serializable> s) { }
}