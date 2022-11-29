// !API_VERSION: 1.0

@SinceKotlin("1.1")
open class C1

typealias C1_Alias = C1

open class C2(val x: Int) {
    @SinceKotlin("1.1")
    constructor() : this(0)
}

typealias C2_Alias = C2

val test1 = C1_Alias()
val test2 = <!NO_VALUE_FOR_PARAMETER!>C2_Alias()<!>

class Test3 : C1_Alias()

class Test4 : <!NO_VALUE_FOR_PARAMETER!>C2_Alias<!>()
