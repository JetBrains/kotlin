// FIR_IDENTICAL
// https://youtrack.jetbrains.com/issue/KT-48157

import TestEnum.*

enum class TestEnum {
    Annotation,
    Collection,
    Set,
    List,
    Map,
    Function,
    Enum
}

fun test() {
    val x1 = Annotation
    val x2 = Collection
    val x3 = Set
    val x4 = List
    val x5 = Map
    val x6 = Function
    val x7 = Enum
}