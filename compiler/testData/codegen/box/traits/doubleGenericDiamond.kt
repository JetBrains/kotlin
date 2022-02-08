// FILE: lib.kt

var result = ""

interface Left
interface Right
class Bottom : Left, Right

interface A<T> {
    fun f(): T? {
        result = "A"
        return null
    }
}

interface B<T : Left> : A<T> {
    override fun f(): T? {
        result = "B"
        return null
    }
}

abstract class C<T> : A<T>

abstract class D<T : Right> : C<T>()

// FILE: box.kt

class Z : D<Bottom>(), B<Bottom>

fun box(): String {
    Z().f()
    return if (result == "B") "OK" else "Fail: $result"
}
