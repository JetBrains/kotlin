fun box(): String {
    val sub = Sub()

    (sub : Super).foo{ }
    if (sub.lastCalled != "super") {
        return "FAIL: ${sub.lastCalled} instead of super"
    }

    sub.foo{ }
    if (sub.lastCalled != "sub") {
        return "FAIL: ${sub.lastCalled} instead of sub"
    }

    return "OK"
}
