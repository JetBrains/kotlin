// !API_VERSION: 1.0

@SinceKotlin("1.1")
class Since_1_1

class C

typealias Since_1_1_Alias = <!API_NOT_AVAILABLE!>Since_1_1<!>

typealias L = List<<!API_NOT_AVAILABLE!>Since_1_1<!>>

@SinceKotlin("1.1")
typealias C_1_1_Alias = C

fun test1(x: <!API_NOT_AVAILABLE!>Since_1_1_Alias<!>) = x

fun test2(x: <!API_NOT_AVAILABLE!>C_1_1_Alias<!>) = x

fun test3(x: List<<!API_NOT_AVAILABLE!>C_1_1_Alias<!>>) = x

fun test4(x: <!API_NOT_AVAILABLE!>L<!>) = x
