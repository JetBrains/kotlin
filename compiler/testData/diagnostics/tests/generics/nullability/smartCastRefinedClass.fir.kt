fun <T : Any?> foo(x: T) {
    if (x is String?) {
        x.length

        if (x != null) {
            x.length
        }
    }

    if (x is String) {
        x.length
    }
}
