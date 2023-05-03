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

class A : S {

    constructor() : super(
            foo(),
            Nested(),
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>Inner<!>(),
            CONST,
            Companion.CONST,
            Nested.CONST,
            Interface.CONST,
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>a<!>,
            <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>b<!>()
    )

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
