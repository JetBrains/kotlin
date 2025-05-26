// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57417

interface HasProperty {
    val property: Int?
}

class Test(delegate: HasProperty) : HasProperty by delegate

fun test(a: Test) {
    if (a.property != null) <!SMARTCAST_IMPOSSIBLE!>a.property<!> + 1
}
