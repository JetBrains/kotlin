// !CHECK_TYPE

public abstract class A<E> {
    fun bar(): String = ""
}

public class B<F> : A<B<F>>()

fun test(b: B<*>) {
    // Here `bar` could have dispatch receiver parameter type 'A<B<Captured(*)>>', but it wouldn't work as
    // since 'b' has type 'A<out B<*>>', so we should approximate dispatch receiver PARAMETER type to make it accept original receiver
    b.bar()
    b.bar() checkType { _<String>() }
}
