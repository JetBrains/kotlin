// ISSUE: KT-64891

fun test(f: (Int) -> Int) {
    <!NO_RECEIVER_ALLOWED!>2.(f)()<!>
}