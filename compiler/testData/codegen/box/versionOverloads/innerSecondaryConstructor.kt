@file:OptIn(ExperimentalVersionOverloading::class)

class VersionedInnerOwner {
    inner class Inner {
        val value: String

        constructor(
            prefix: String = "O",
            @IntroducedAt("1") suffix: String = "K",
        ) {
            value = prefix + suffix
        }
    }
}

fun box(): String {
    if (VersionedInnerOwner().Inner().value != "OK") return "FAIL default"
    if (VersionedInnerOwner().Inner("A", "B").value != "AB") return "FAIL explicit"
    return "OK"
}
