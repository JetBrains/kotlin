enum class A {
    A1, A2
}

enum class B {
    B1, B2
}

fun test(a1: A, a2: A, b: B) {
    typesHaveCommonSubtype(a1, a2)
    typesHaveCommonSubtype(a1, A.A1)

    typesHaveNoCommonSubtype(a1, b)
    typesHaveNoCommonSubtype(A.A1, b)
}