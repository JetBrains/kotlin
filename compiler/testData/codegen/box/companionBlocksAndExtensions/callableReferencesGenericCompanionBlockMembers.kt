// WITH_STDLIB
// LANGUAGE: +CompanionBlocks +CompanionExtensions

class GenericHost<T> {
    companion {
        fun foo(value: String): String = "foo:$value"
        val marker: String = "marker"
    }
}

typealias StringHost = GenericHost<String>

fun box(): String {
    val directFun: (String) -> String = GenericHost::foo
    val directProp: () -> String = GenericHost::marker
    val aliasFun: (String) -> String = StringHost::foo
    val aliasProp: () -> String = StringHost::marker

    val directResult = directFun("O")
    if (directResult != "foo:O") return "FAIL: direct fun ref: $directResult"

    val directPropResult = directProp()
    if (directPropResult != "marker") return "FAIL: direct prop ref: $directPropResult"

    val aliasFunResult = aliasFun("K")
    if (aliasFunResult != "foo:K") return "FAIL: alias fun ref: $aliasFunResult"

    val aliasPropResult = aliasProp()
    if (aliasPropResult != "marker") return "FAIL: alias prop ref: $aliasPropResult"

    return "OK"
}
