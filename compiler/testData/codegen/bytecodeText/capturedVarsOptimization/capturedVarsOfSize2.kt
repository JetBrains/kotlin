fun box(): String {
    var xl = 0L     // Long, size 2
    var xi = 0      // Int, size 1
    var xd = 0.0    // Double, size 2

    run {
        xl++
        xd += 1.0
        xi++
    }

    run {
        run {
            xl++
            xd += 1.0
            xi++
        }
    }

    return "OK"
}

// 0 NEW
