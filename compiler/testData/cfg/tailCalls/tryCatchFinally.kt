fun test() : Unit {
    try {
        test()
    } catch (any : Exception) {
        test()
    } finally {
        test()
    }
}