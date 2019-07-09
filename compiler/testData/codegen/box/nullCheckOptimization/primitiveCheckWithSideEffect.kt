var result = "fail 1"

fun withSideEffect() : Int {
    result = "OK"
    return 42
}

fun box(): String {
    if (withSideEffect() == null) {
        return "fail 2"
    }

    if (result != "OK") {
        return "fail 3"
    }

    result = "fail 4"
    if (withSideEffect() != null) {
        return result
    }

    return result
}