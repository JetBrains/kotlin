interface Left
interface Right
class Bottom : Left, Right

interface A<T> {
    fun f(): T? {
        return null
    }
}

interface B<T : Left> : A<T> {}

abstract class C<T> : A<T>

abstract class D<T : Right> : C<T>()

class Z : D<Bottom>(), B<Bottom>


fun box(): String {
    Z().f()
    return "OK"
}
