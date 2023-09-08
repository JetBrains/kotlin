// ISSUE: KT-55168
fun foo(arg: Boolean) = <!NEW_INFERENCE_ERROR!>buildList {
    if (arg) {
        removeLast()
    } else {
        add(42)
    }
}<!>
