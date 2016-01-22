package test

private const val packageProp = "O"

internal inline fun packageInline(p: (String) -> String): String {
    return p(packageProp)
}

internal fun samePackageCall(): String {
    return packageInline { it + "K"}
}