// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
class A {
    fun forEach() = this
    fun forEach(i: Int) = this
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.HidesMembers
fun A.forEach(i: Int) = i

class B {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.HidesMembers
    fun A.forEach() = this@B

    fun test(a: A) {
        a.forEach() checkType { _<A>() } // todo

        with(a) {
            forEach() checkType { _<A>() } // todo
        }
    }
}

fun test2(a: A) {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.HidesMembers
    fun A.forEach() = ""

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.HidesMembers
    fun A.forEach(i: Int) = ""

    a.forEach() checkType { _<String>() }
    a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>forEach<!>(1)

    with(a) {
        forEach() checkType { _<String>() }
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>forEach<!>(1)
    }
}