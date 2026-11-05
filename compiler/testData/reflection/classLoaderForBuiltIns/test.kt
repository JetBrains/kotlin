fun test() {
    check(String::class.supertypes.isNotEmpty())

    check(Int::class.members.first { it.name == "rangeTo" }.returnType.classifier == IntRange::class)
    check(Long::class.members.first { it.name == "rangeTo" }.returnType.classifier == LongRange::class)
    check(Char::class.members.first { it.name == "rangeTo" }.returnType.classifier == CharRange::class)
    check(Array<Any>::class.members.first { it.name == "set" }.returnType.classifier == Unit::class)
}
