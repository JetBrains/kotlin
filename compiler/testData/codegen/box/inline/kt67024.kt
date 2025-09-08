inline fun <reified T> id(x: T) = x

fun test1(block: (String) -> String = ::id)  = block("O")
inline fun test2(block: (String) -> String = ::id)  = block("K")

fun box() : String {
    return test1() + test2()
}
