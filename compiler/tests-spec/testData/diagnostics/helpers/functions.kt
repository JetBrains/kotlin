fun _funWithoutArgs(): Int {
    return Any().hashCode().toInt()
}

fun _funWithAnyArg(value_1: Any): Int {
    return value_1.hashCode()
}
