// WITH_STDLIB

fun box(): String {
    run {
        run {
            var x = 0
            run { ++x }
            if (x == 0) return "fail"
        }

        run {
            var x = 0
            run { x++ }
            if (x == 0) return "fail"
        }
    }

    return "OK"
}