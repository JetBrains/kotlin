@file:OptIn(ExperimentalVersionOverloading::class)

private fun privateFun(x: Int): Int = x
internal inline fun inlineFun(@IntroducedAt("1") x: Int = 0): Int = privateFun(x)

private fun privateFunWithVersionedParameter(@IntroducedAt("1") x: Int = 0): Int = x
internal inline fun inlineFunWithPrivateVersionedParameter(x: Int): Int = privateFunWithVersionedParameter(x)

private fun privateDefaultValue(): Int = 0
fun functionWithPrivateDefaultValue(@IntroducedAt("1") x: Int = privateDefaultValue()): Int = x

class WithPrivateDefault {
    private fun defaultValue(): Int = 0

    fun value(@IntroducedAt("1") x: Int = defaultValue()): Int = x
}

fun box(): String {
    if (inlineFun() != 0) return "fail9"
    if (inlineFunWithPrivateVersionedParameter(0) != 0) return "fail10"
    if (functionWithPrivateDefaultValue() != 0) return "fail11"
    if (WithPrivateDefault().value() != 0) return "fail15"

    return "OK"
}
