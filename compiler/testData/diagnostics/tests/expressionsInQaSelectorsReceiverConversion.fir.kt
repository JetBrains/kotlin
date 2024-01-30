// ISSUE: KT-64891
// FIR_DUMP

fun test(f: (Int) -> Int) {
    <!NO_RECEIVER_ALLOWED!>2.(f)()<!>
}

typealias TA = Int.() -> Int

fun rest(f: TA) {
    2.(f)()
}
