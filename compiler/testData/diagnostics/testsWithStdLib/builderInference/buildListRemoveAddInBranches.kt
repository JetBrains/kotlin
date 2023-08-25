// ISSUE: KT-55168
fun foo(arg: Boolean) = buildList {
    if (arg) <!TYPE_MISMATCH!>{
        <!TYPE_MISMATCH!>removeLast()<!>
    }<!> else {
        add(42)
    }
}
