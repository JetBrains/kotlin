@file:OptIn(ExperimentalVersionOverloading::class)

fun inTrailing(
    x: String,
    @IntroducedAt("1") y: Int = 1,
    block: () -> String = null!!,
) = "$x/$y/${block()}"

fun inArgument(
    x: String,
    @IntroducedAt("1") y: Int = 1,
    @IntroducedAt("2") z: () -> Int = null!!,
    block: () -> String
) = "$x/$y/${z()}/${block()}"
