// !DIAGNOSTICS: -UNUSED_PARAMETER

class A(
        n: Nested = foo(),
        n2: Nested = Nested(),
        inn: Inner = null!!,
        inn2: Inner = <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>(),
        i: Interface = null!!,
        c: Int = CONST,
        cc: Int = Companion.CONST,
        cn: Int = Nested.CONST,
        ci: Int = Interface.CONST,
        t1: Int = <!UNRESOLVED_REFERENCE!>a<!>,
        t2: Int = <!UNRESOLVED_REFERENCE!>b<!>()
) {

    constructor(
            dummy: Int,
            n: Nested = foo(),
            n2: Nested = Nested(),
            inn: Inner = null!!,
            inn2: Inner = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>Inner<!>(),
            i: Interface = null!!,
            c: Int = CONST,
            cc: Int = Companion.CONST,
            cn: Int = Nested.CONST,
            ci: Int = Interface.CONST,
            t1: Int = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>a<!>,
            t2: Int = <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>b<!>()
    ) : this(
        foo(),
        Nested(),
        inn,
        <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>Inner<!>(),
        i,
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
