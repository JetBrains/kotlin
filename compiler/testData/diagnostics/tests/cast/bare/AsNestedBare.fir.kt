interface Tr
interface G<T>

fun test(tr: Tr): Any {
    return tr as G<G>
}

fun test1(tr: Tr): Any {
    return tr as G.(G) -> G
}