// IS_APPLICABLE: false
enum class Type {
    HYDRO,
    PYRO
}

fun select(t: Type): Int {
    return <caret>when (t) {
        Type.HYDRO -> 1
        Type.PYRO -> 42
    }
}