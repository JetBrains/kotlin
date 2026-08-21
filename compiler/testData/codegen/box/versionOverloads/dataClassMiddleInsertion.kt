@file:OptIn(ExperimentalVersionOverloading::class)

data class DataWithMiddleVersion(
    val first: Int,
    @IntroducedAt("1") val middle: String = "M",
    val last: String = "K",
)

data class DataWithPrivateVersion(
    val first: Int,
    @IntroducedAt("1") private val secret: String = "S",
    val last: String = "K",
) {
    fun describe(): String = "$first/$secret/$last"
}

fun box(): String {
    val data = DataWithMiddleVersion(1)
    if (data.component1() != 1 || data.component2() != "M" || data.component3() != "K") return "FAIL components"

    val (first, middle, last) = data
    if (first != 1 || middle != "M" || last != "K") return "FAIL destructuring"
    if (data.copy(last = "!") != DataWithMiddleVersion(1, "M", "!")) return "FAIL copy default"
    if (data.copy(first = 2, middle = "O", last = "K") != DataWithMiddleVersion(2, "O", "K")) return "FAIL copy all"

    val privateData = DataWithPrivateVersion(1)
    if (privateData.describe() != "1/S/K") return "FAIL private property"
    if (privateData.copy(first = 2).describe() != "2/S/K") return "FAIL private copy"
    if (privateData.copy(first = 2) != DataWithPrivateVersion(2)) return "FAIL private equality"

    return "OK"
}
