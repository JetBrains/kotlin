// ISSUE: KT-86646

data class Some<T, U : T & Any>(val x: U, val y: T & Any)

fun box(): String {
    val s = Some("O", "K")
    return s.x + s.y
}
