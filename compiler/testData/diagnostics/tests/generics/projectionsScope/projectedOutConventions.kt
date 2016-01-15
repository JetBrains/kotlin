// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    operator fun plus(x: T): A<T> = this
    operator fun set(x: Int, y: T) {}
    operator fun get(x: T) = 1
}

fun test(a: A<out CharSequence>) {
    a <!MEMBER_PROJECTED_OUT(public final operator fun plus\(x: T\): A<T> defined in A; A<out kotlin.CharSequence>)!>+<!> ""
    <!MEMBER_PROJECTED_OUT(public final operator fun set\(x: kotlin.Int, y: T\): kotlin.Unit defined in A; A<out kotlin.CharSequence>)!>a[1]<!> = ""
    <!MEMBER_PROJECTED_OUT(public final operator fun get\(x: T\): kotlin.Int defined in A; A<out kotlin.CharSequence>)!>a[""]<!>
}
