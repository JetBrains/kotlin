fun foo() {
    var some: String? = null
    some = "alpha"
    try {
        some = "omega"
    }
    catch (e: Exception) {}
    some.length
}