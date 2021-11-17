interface I
data class Foo(val i: Int) : I
data class Bar(val s: String)

fun test(f1: Foo, f2: Foo, f3: Foo?, b1: Bar, b2: Bar?, i: I) {
    typesHaveCommonSubtype(f1, f2)
    typesHaveCommonSubtype(f1, f3)
    typesHaveCommonSubtype(f3, b2)
    typesHaveCommonSubtype(f1, i)

    typesHaveNoCommonSubtype(f1, b1)
    typesHaveNoCommonSubtype(f1, b2)
    typesHaveNoCommonSubtype(b1, i)
}
