fun box(): String {
    val result = WeirdComparator.max<String>({ a, b -> a.length - b.length }, "java", "kotlin")
    if (result != "kotlin") return "Wrong: $result"

    val result2 = WeirdComparator.max2<String>({ a, b -> a.length - b.length }, "java", "kotlin")
    if (result2 != "kotlin") return "Wrong: $result"

    return "OK"
}