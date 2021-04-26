fun main(x1: Double?, range: ClosedRange<Double>) {
    when (x1) {
        null -> throw Exception()
        <!ARGUMENT_TYPE_MISMATCH!>in range<!> -> {} // error, no smartcast from previous branch, OK in OI
    }

    when {
        x1 == null -> throw Exception()
        x1 in range -> {}
    }
}
