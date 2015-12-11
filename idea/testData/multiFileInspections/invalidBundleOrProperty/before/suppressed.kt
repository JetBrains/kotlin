package suppressed

@Suppress("InvalidBundleOrProperty")
fun testSuppressedOnFun() {
    K.message("foo.bar")
    J.message("foo.bar")
}