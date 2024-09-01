// ISSUE: KT-57417
// LANGUAGE: +UnstableSmartcastOnDelegatedProperties

interface HasProperty {
    val property: Int?
}

class Test(delegate: HasProperty) : HasProperty by delegate

fun test(a: Test) {
    if (a.property != null) <!SMARTCAST_IMPOSSIBLE!>a.property<!> + 1
}
