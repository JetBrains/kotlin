// ISSUE: KT-57417

interface HasProperty {
    val property: Int?
}

class Test(delegate: HasProperty) : HasProperty by delegate

fun test(a: Test) {
    if (a.property != null) <!DEPRECATED_SMARTCAST_ON_DELEGATED_PROPERTY!>a.property<!> + 1
}
