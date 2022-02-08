fun test() {
    typesHaveCommonSubtype(1, 1)
    typesHaveCommonSubtype("", "")
    typesHaveCommonSubtype(null, null)

    typesHaveNoCommonSubtype("", null)
    typesHaveNoCommonSubtype(1, 1L)
    typesHaveNoCommonSubtype(1, 1.0)
    typesHaveNoCommonSubtype(1, "")
}