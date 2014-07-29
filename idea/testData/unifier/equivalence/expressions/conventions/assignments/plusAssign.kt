// DISABLE-ERRORS
class A(var n: Int) {
    fun plusAssign(m: Int) {
        n += m
    }
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