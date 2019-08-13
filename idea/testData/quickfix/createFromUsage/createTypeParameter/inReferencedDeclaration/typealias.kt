// "Create type parameter in type alias 'G'" "true"

class C
typealias G = C

fun <T> a(g: G<T<caret>>) = Unit