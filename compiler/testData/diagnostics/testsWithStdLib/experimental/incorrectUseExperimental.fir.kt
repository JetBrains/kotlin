// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

annotation class NotAMarker

@OptIn
fun f1() {}

@OptIn(NotAMarker::class)
fun f2() {}
