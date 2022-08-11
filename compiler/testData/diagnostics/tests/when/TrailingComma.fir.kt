fun singleConditionWithTrailingComma(x: Int) {
    when(x) {
        0, -> {}
        else -> {}
    }
}

fun multipleConditionsWithoutTrailingComma(x: Int) {
    when(x) {
        0, 1 -> {}
        else -> {}
    }
}

fun multipleConditionsWithTrailingComma(x: Int) {
    when(x) {
        0, 1, -> {}
        else -> {}
    }
}

fun multipleConditionsWithMissedCondition(x: Int) {
    when(x) {
        0,  , 1 -> {}
        else -> {}
    }
}

fun elseWithTrailingComma(x: Int) {
    when(x) {
        0, 1 -> {}
        else , -> {}
    }
}