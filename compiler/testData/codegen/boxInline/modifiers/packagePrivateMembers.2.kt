package test

private val packageProp = "O"

private fun packageFun() = "K"

inline fun packageInline(p: (String, String) -> String): String {
    return p(packageProp, packageFun())
}

fun samePackageCall(): String {
    return packageInline { s, s2 -> s + s2 }
}