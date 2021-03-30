// "Change to val" "true"
class Test {
    var foo: Int<caret>
        get() {
            return 1
        }
}
/* FIR_COMPARISON */