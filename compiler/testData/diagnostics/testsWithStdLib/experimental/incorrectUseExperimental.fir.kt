// !USE_EXPERIMENTAL: kotlin.Experimental

annotation class NotAMarker

@UseExperimental
fun f1() {}

@UseExperimental(NotAMarker::class)
fun f2() {}
