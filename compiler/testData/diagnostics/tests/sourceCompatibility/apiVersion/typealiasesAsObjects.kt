// FIR_IDENTICAL
// !API_VERSION: 1.0

@SinceKotlin("1.1")
object Since_1_1 {
    val x = 42
}

typealias Since_1_1_Alias = <!API_NOT_AVAILABLE!>Since_1_1<!>

val test1 = <!API_NOT_AVAILABLE!>Since_1_1_Alias<!>
val test2 = <!API_NOT_AVAILABLE!>Since_1_1_Alias<!>.x