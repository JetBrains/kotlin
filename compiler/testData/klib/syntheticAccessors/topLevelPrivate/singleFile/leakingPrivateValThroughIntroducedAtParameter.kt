@file:OptIn(ExperimentalVersionOverloading::class)

private val ok = "OK"

internal inline fun internalInlineFun(
    @IntroducedAt("1") ok1: String = ok,
    @IntroducedAt("2") ok2: String = ok1
) = ok2

fun box(): String = internalInlineFun()
