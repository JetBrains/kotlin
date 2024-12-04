fun interface IFoo {
    fun foo(i: Int)
}

fun interface IFoo2 : IFoo

object A
object B

operator fun A.get(i: IFoo) = 1
operator fun A.set(i: IFoo, newValue: Int) {}

operator fun B.get(i: IFoo) = 1
operator fun B.set(i: IFoo2, newValue: Int) {}

fun withVararg(vararg xs: Int) = 42

fun test1() {
    A[::withVararg] += 1
}

fun test2() {
    B[::withVararg] += 1
}

fun test3(fn: (Int) -> Unit) {
    A[fn] += 1
}

fun test4(fn: (Int) -> Unit) {
    if (fn is IFoo) {
        A[fn] += 1
    }
}

fun test5(a: Any) {
    a as (Int) -> Unit
    A[a] += 1
}

fun test6(a: Any) {
    a as (Int) -> Unit
    a as IFoo
    A[a] += 1
}
