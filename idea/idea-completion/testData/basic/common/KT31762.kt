enum class A { AAA }
enum class E(val one: A, val two: A) {
    EE(A.<caret>, A.AAA)
}

// EXIST: AAA