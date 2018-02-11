// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    operator fun plus(x: Out<T>): A<T> = this
    operator fun set(x: Int, y: Out<T>) {}
    operator fun get(x: Out<T>) = 1
}

class Out<out F>

fun test(a: A<out CharSequence>, y: Out<CharSequence>) {
    a + <!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>y<!>
    a[1] = <!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>y<!>
    a[<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>y<!>]

    a + Out<Nothing>()
    a[1] = Out<Nothing>()
    a[Out<Nothing>()]
}
