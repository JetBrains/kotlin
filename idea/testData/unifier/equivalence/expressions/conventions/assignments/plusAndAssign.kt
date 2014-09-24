// DISABLE-ERRORS
class A(val n: Int) {
    fun plus(m: Int) = A(n + m)
}

class Foo {
    {
        var a = A(0)
        <selection>a += 2</selection>
        a = a + 2
        a + 2
        a.plus(2)
        a.plusAssign(2)
    }
}