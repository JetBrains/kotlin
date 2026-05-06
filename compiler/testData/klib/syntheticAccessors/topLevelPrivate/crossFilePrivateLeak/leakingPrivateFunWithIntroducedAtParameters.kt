// FILE: A.kt
@file:OptIn(ExperimentalVersionOverloading::class)

private fun privateFun(
    @IntroducedAt("1") ok1: String = "OK",
    @IntroducedAt("2") ok2: String = ok1
) = ok2

internal inline fun internalInlineFun() = privateFun()

// FILE: B.kt
fun box(): String = internalInlineFun()
