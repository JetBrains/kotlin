// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

class A
class B

context(A)
fun tooo() = Unit

context(B)
fun tooo() = Unit

context(A, B)
fun tooo() = Unit

context(B, A)
fun tooo() = Unit