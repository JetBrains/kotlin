// !DIAGNOSTICS: -UNUSED_PARAMETER
// See KT-7296
interface Out<out F>
interface B<T> : Out<Out<T>>

fun foo(x : B<*>) {
    bar1(x)
    bar2(x)
}

fun bar1(x : Out<Out<*>>) { }
fun bar2(x : Out<*>) { }
