// FIR_IDENTICAL
typealias N = Nothing

fun <!ABBREVIATED_NOTHING_RETURN_TYPE!>testFun<!>(): N = null!!
val <!ABBREVIATED_NOTHING_PROPERTY_TYPE!>testVal<!>: N = null!!
val <!ABBREVIATED_NOTHING_PROPERTY_TYPE!>testValWithGetter<!>: N get() = null!!

fun testFunN(): Nothing = null!!
val testValN: Nothing = null!!
val testValWithGetterN: Nothing get() = null!!
