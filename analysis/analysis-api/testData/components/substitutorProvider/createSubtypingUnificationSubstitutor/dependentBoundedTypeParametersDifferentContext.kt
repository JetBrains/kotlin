// WITH_STDLIB

fun <S: Number> target(targetType: List<S>) {
    target<caret_1_target>Type
}

fun <T, R: Number> candidate(candidateType: List<Pair<T, R>>) {
    candi<caret_1_base>dateType
}