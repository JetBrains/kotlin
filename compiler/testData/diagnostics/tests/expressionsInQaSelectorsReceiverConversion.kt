// ISSUE: KT-64891

fun test(f: (Int) -> Int) {
    2.<!NO_RECEIVER_ALLOWED!>(f)<!>()
}
