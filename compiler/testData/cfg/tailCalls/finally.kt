tailRecursive fun test() : Int {
    try {
        // do nothing
    } finally {
        test()
    }
}
