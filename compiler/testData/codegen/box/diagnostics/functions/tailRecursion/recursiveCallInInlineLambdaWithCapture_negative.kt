// DONT_TARGET_EXACT_BACKEND: JVM_IR
// ===== Inline lambda capturing receiver in member function =====
class Counter(val value: Int) {
    <!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun countTo(target: Int, steps: Int): Int {
        if (value + steps >= target) return steps
        run {
            return <!NON_TAIL_RECURSIVE_CALL!>countTo<!>(target, steps + 1)
        }
    }<!>
}

fun box(): String {
    val countTo = Counter(10).countTo(42, 1)
    if (countTo != 32) return "Fail countTo: $countTo"

    return "OK"
}
