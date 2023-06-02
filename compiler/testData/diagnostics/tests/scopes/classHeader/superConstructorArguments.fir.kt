// !DIAGNOSTICS: -UNUSED_PARAMETER

open class S(
        n: A.Nested,
        n2: A.Nested,
        inn: A.Inner,
        c: Int,
        cc: Int,
        cn: Int,
        ci: Int,
        t1: Int,
        t2: Int
)

class A : S (
    foo(),
    Nested(),
    <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>(),
    CONST,
    Companion.CONST,
    Nested.CONST,
    Interface.CONST,
    <!UNRESOLVED_REFERENCE!>a<!>,
    <!UNRESOLVED_REFERENCE!>b<!>()
) {

    class Nested {
        companion object {
            const val CONST = 2
        }
    }

    inner class Inner

    interface Interface {
        companion object {
            const val CONST = 3
        }
    }

    val a = 1
    fun b() = 2

    companion object {
        const val CONST = 1
        fun foo(): Nested = null!!
    }
}
