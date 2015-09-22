package test

private val packageProp = "O"

private fun packageFun() = "K"

internal inline fun internalPackageInline(p: (String, String) -> String): String {
    return p(packageProp, packageFun())
}

internal fun internalSamePackageCall(): String {
    return packageInline { s, s2 -> s + s2 }
}

fun packageInline(p: (String, String) -> String): String {
    return internalPackageInline(p)
}

fun samePackageCall(): String {
    return internalSamePackageCall()
}