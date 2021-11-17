fun test() {
    typesHaveCommonSubtype(listOf(1), listOf(1))
    typesHaveCommonSubtype(listOf(1), setOf(1))
    typesHaveCommonSubtype(listOf(1), mutableSetOf(1))

    typesHaveNoCommonSubtype(listOf(1), listOf(""))
    typesHaveNoCommonSubtype(listOf(1), setOf(""))
    typesHaveNoCommonSubtype(listOf(1), mutableSetOf(""))
}