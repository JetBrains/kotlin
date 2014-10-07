fun whileLoop(x: Int?) {
    @outer while (x != 0) {
        while (x != 1) {
            if (x == 2) continue @outer
        }
        <!TYPE_MISMATCH!>x<!> : Int
    }
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
}

fun doWhileLoop(x: Int?) {
    @outer while (x != 0) {
        do {
            if (x == 2) continue @outer
        } while (x == null)
        <!TYPE_MISMATCH!>x<!> : Int
    }
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
}

fun whileLoopContinueInnerOuter(x: Int?) {
    @outer while (x != 0) {
        @inner while (x != 1) {
            while (x != 2) {
                if (x == 3) continue @inner
            }
            <!TYPE_MISMATCH!>x<!> : Int
        }
        <!DEBUG_INFO_SMARTCAST!>x<!> : Int
    }
    <!DEBUG_INFO_SMARTCAST!>x<!> : Int
}
