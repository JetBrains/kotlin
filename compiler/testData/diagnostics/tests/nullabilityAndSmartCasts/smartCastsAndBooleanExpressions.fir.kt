fun foo(b: Boolean?, c: Boolean) {
    if (b != null && b) {}
    if (b == null || b) {}
    if (b != null) {
        if (b && c) {}
        if (b || c) {}
    }
}
