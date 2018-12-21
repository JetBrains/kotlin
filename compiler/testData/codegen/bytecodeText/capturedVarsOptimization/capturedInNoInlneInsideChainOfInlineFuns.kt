// IGNORE_BACKEND: JVM_IR
fun runNoInline(f: () -> Unit) = f()

fun test() {
    var x = 0
    run {
        run {
            runNoInline {
                run {
                    ++x
                }
            }
        }
    }
}

// 1 NEW kotlin/jvm/internal/Ref\$IntRef
// 2 GETFIELD kotlin/jvm/internal/Ref\$IntRef\.element
// 2 PUTFIELD kotlin/jvm/internal/Ref\$IntRef\.element
