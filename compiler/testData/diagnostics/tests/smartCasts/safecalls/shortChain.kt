fun calc(x: List<String?>): Int {
    // All should work fine
    x.get(0)?.subSequence(0, 1)
    return x.size()
}
