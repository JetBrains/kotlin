class X

private operator fun X?.plus(p: Int) = X()

class C {
    fun foo(x: X) {
        x.plus(1)sda
    }
}
