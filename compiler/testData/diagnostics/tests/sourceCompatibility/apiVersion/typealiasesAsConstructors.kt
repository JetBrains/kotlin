// !API_VERSION: 1.0

@SinceKotlin("1.1")
open class C1

typealias C1_Alias = <!API_NOT_AVAILABLE!>C1<!>

open class C2(val x: Int) {
    @SinceKotlin("1.1")
    constructor() : this(0)
}

typealias C2_Alias = C2

val test1 = <!UNRESOLVED_REFERENCE!>C1_Alias<!>()
val test2 = C2_Alias(<!NO_VALUE_FOR_PARAMETER!>)<!>

class Test3 : <!UNRESOLVED_REFERENCE, API_NOT_AVAILABLE, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>C1_Alias<!>()

class Test4 : C2_Alias(<!NO_VALUE_FOR_PARAMETER!>)<!>