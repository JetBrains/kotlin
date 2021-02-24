fun main(x1: Double?, range: ClosedRange<Double>) {
    when (x1) {
        null -> throw Exception()
        in range -> {} // error, no smartcast from previous branch, OK in OI
    }

    when {
        x1 == null -> throw Exception()
        <!DEBUG_INFO_SMARTCAST!>x1<!> in range -> {}
    }
}
