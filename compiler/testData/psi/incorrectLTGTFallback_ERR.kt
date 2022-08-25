// ISSUE: KT-53719

fun main() {
    foo<Int> label@ 1
    foo<Int> label@ (2)
    foo<Int> label@3
    foo<Int> label@({})
}
