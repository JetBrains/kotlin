// !DIAGNOSTICS: -UNUSED_PARAMETER

class A(
        n: Nested = foo(),
        n2: Nested = Nested(),
        inn: Inner = null!!,
        inn2: Inner = Inner(),
        i: Interface = null!!,
        c: Int = CONST,
        cc: Int = Companion.CONST,
        cn: Int = Nested.CONST,
        ci: Int = Interface.CONST,
        t1: Int = a,
        t2: Int = b()
) {

    constructor(
            dummy: Int,
            n: Nested = foo(),
            n2: Nested = Nested(),
            inn: Inner = null!!,
            inn2: Inner = Inner(),
            i: Interface = null!!,
            c: Int = CONST,
            cc: Int = Companion.CONST,
            cn: Int = Nested.CONST,
            ci: Int = Interface.CONST,
            t1: Int = a,
            t2: Int = b()
    ) : this(
        foo(),
        Nested(),
        inn,
        Inner(),
        i,
        CONST,
        Companion.CONST,
        Nested.CONST,
        Interface.CONST,
        <!UNRESOLVED_REFERENCE!>a<!>,
        <!UNRESOLVED_REFERENCE!>b<!>()
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
