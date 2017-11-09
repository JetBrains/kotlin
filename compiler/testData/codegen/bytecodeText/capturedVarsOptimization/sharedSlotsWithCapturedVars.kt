// WITH_RUNTIME

fun box(): String {
    run {
        run {
            var x1 = 0
            run { ++x1 }
            if (x1 == 0) return "fail"
        }

        run {
            var x2 = 0
            run { x2++ }
            if (x2 == 0) return "fail"
        }
    }

    return "OK"
}


// Shared variable slots (x1, x2):
// 4 ILOAD 0
// 4 ISTORE 0

// Temporary variable slots for 'x2++':
// 0 ILOAD 1
// 1 ISTORE 1

// 0 NEW
// 0 GETFIELD
// 0 PUTFIELD