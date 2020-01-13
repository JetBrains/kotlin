// !API_VERSION: 1.0

@SinceKotlin("1.1")
object Since_1_1 {
    val x = 42
}

typealias Since_1_1_Alias = Since_1_1

val test1 = Since_1_1_Alias
val test2 = Since_1_1_Alias.x