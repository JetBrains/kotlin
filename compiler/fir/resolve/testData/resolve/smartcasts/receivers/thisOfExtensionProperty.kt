// !DUMP_CFG
interface A

interface B {
    val b: Boolean
}

val A.check_1: Boolean
    get() = this is B && b

val A.check_2: Boolean
    get() = this is B && this.b
