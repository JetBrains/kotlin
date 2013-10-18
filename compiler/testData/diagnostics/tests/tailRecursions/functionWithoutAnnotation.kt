fun withoutAnnotation(x : Int) : Int {
    if (x > 0) {
        return 1 + withoutAnnotation(x - 1)
    }
    return 0
}