fun main(x1: Double?, range: ClosedRange<Double>) {
    when (x1) {
        null -> throw Exception()
        <!INAPPLICABLE_CANDIDATE!>in<!> range -> {} // error, no smartcast from previous branch, OK in OI
    }

    when {
        x1 == null -> throw Exception()
        x1 in range -> {}
    }
}
