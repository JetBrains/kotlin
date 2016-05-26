typealias S = String

fun test1(x: Any) = x is S
fun test2(x: Any) = x as S
fun test3(x: Any) = x as? S
