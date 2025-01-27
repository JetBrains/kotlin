// RUN_PIPELINE_TILL: FRONTEND
interface A<out T>

interface MutableA<T> : A<T> {
    fun add(x: T)
}

interface MutableString : MutableA<String>

fun test(a: A<String>) {
    (a as? MutableA)?.add(<!MEMBER_PROJECTED_OUT!>""<!>)
    (a as MutableA).add(<!MEMBER_PROJECTED_OUT!>""<!>)
}

fun test2(a: A<String>) {
    val b = a as MutableString
    b.add("")
}

fun test3(a: A<String>) {
    if (a is MutableA) {
        a.add(<!MEMBER_PROJECTED_OUT!>""<!>)
    }
}
