interface Left
interface Right
class Bottom : Left, Right

interface A<T> {
    fun f(): T? {
        return null
    }
}

interface B<T : Left> : A<T> {
    override fun f(): T? {
        return null
    }
}

abstract class C<T> : A<T>

abstract class D<T : Right> : C<T>()

// We should not have intersection override f() in this class
class Z : D<Bottom>(), B<Bottom>