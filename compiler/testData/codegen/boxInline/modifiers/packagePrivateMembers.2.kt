package test

/*TODO rollback when supported*/
/*private*/ val packageProp = "O"

/*private*/ fun packageFun() = "K"

internal inline fun packageInline(p: (String, String) -> String): String {
    return p(packageProp, packageFun())
}

internal fun samePackageCall(): String {
    return packageInline { s, s2 -> s + s2 }
}