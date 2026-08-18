@file:OptIn(ExperimentalVersionOverloading::class)

fun <T> genericCallableReference(
    value: T,
    @IntroducedAt("1") suffix: String = "!",
): String = "$value$suffix"

class GenericCallableReferenceHolder {
    fun <T> render(
        value: T,
        @IntroducedAt("1") suffix: String = "!",
    ): String = "$value$suffix"
}

fun box(): String {
    if (genericCallableReference("OK") != "OK!") return "FAIL top-level default"

    val topLevelReference: (String, String) -> String = ::genericCallableReference
    if (topLevelReference("O", "K") != "OK") return "FAIL top-level reference"

    val holder = GenericCallableReferenceHolder()
    if (holder.render("OK") != "OK!") return "FAIL member default"

    val memberReference: (GenericCallableReferenceHolder, String, String) -> String =
        GenericCallableReferenceHolder::render
    if (memberReference(holder, "O", "K") != "OK") return "FAIL member reference"

    return "OK"
}
