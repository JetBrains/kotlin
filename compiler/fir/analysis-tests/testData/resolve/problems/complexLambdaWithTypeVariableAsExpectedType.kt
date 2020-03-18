fun <T> id(x: T): T = x
fun <K> select(x: K, y: K): K = TODO()

fun test() {
    select(id { it.inv() }, id<(Int) -> Unit> { })
}