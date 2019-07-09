enum A { X, Y }

private enum B {
    X, Y
}

internal Q {
    X, Y
}

fun foo() {
    // No recovery here
    enum A { X, Y }

    private enum B {
            X, Y
    }
}
