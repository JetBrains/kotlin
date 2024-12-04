// ISSUE: KT-53719

fun main() {
    foo<Int> label@ { x -> x }
    foo<Int> label@{ x -> x }
    foo<Int>label@ { x -> x }
    foo<Int>label@{ x -> x }
    foo<Int>label@
    { x -> x }

    foo<Int>/* */label@ { x -> x }
    foo<Int>/* */label@{ x -> x }
    foo<Int>label@/* */{ x -> x }
    foo<Int> label@/* */{ x -> x }
    foo<Int> label@/* */
    { x -> x }

    foo<Int> @Ann("") label@ { x -> x }
    foo<Int>/* */@Ann("") label@ { x -> x }
    foo<Int>@Ann("")/* */label@ { x -> x }
    foo<Int> @Ann("") label@/* */{ x -> x }
    foo<Int> @Ann("") @Ann("") @Ann("") label@/* */ { x -> x }
    foo<Int> @Ann("") @Ann("") @Ann("") label@/* */
    { x -> x }
}
