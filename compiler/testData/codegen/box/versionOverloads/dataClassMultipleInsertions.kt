@file:OptIn(ExperimentalVersionOverloading::class)

data class DataWithMultipleInsertions(
    val before: Int = 0,
    @IntroducedAt("1") val firstInserted: String = "A",
    val middle: Int = 1,
    @IntroducedAt("2") val secondInserted: String = "B",
    val after: String = "K",
)

fun box(): String {
    val data = DataWithMultipleInsertions()
    if (data.component1() != 0 || data.component2() != "A" || data.component3() != 1) return "FAIL components 1"
    if (data.component4() != "B" || data.component5() != "K") return "FAIL components 2"

    val (before, firstInserted, middle, secondInserted, after) = data
    if (before != 0 || firstInserted != "A" || middle != 1 || secondInserted != "B" || after != "K") {
        return "FAIL destructuring"
    }
    if (data.copy(firstInserted = "X", secondInserted = "Y") != DataWithMultipleInsertions(firstInserted = "X", secondInserted = "Y")) {
        return "FAIL copy inserted"
    }
    if (data.copy(middle = 2, after = "!") != DataWithMultipleInsertions(middle = 2, after = "!")) {
        return "FAIL copy surrounding"
    }
    return "OK"
}
