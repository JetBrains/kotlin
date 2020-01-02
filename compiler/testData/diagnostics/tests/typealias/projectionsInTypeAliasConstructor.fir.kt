class In<in T>(val x: Any)

typealias InAlias<T> = In<T>

val test1 = In<out String>("")
val test2 = InAlias<out String>("")
