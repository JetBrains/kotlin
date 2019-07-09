// IGNORE_BACKEND: JVM_IR
fun test() {
    var x = 0
    run {
        run {
            run {
                ++x
            }
        }
    }
}

// 0 NEW
// 0 GETFIELD
// 0 PUTFIELD
