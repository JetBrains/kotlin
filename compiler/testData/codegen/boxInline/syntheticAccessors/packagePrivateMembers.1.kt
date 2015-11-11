import test.*

fun box(): String {
    val packageResult = packageInline { a, b -> a + b }
    if (packageResult != "OK") return "package inline fail: $packageResult"

    val samePackageResult = samePackageCall()
    if (samePackageResult != "OK") return "same package inline fail: $samePackageResult"

    return "OK"
}
