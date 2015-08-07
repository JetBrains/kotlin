enum class E1 { F, S, T }

enum class E2 {
    F, S, T
}

enum class E3 {
    F, S, T
}

enum class E4 {
    F {
    },
    S, T
}

enum class E5 { F, S {}, T }

enum class E6 {
    F, S, T {}
}

enum class E7 {
    F {

    }, S, T
}