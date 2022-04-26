// WITH_STDLIB

fun expandMaskConditionsAndUpdateVariableNodes(validOffsets: Collection<Int>) {}

fun main(x: List<Int>, y: Int) {
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION!>expandMaskConditionsAndUpdateVariableNodes<!>(
        x.mapTo(mutableSetOf()) { y }
    )
}
