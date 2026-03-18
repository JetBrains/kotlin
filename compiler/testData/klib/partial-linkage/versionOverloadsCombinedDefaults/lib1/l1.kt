@file:OptIn(kotlin.ExperimentalVersionOverloading::class)

fun fooMiddleInsertFun(
    x: Int,
    @IntroducedAt("1") y: Int = 1,
    @IntroducedAt("2") z: Int = 10,
): Int = x + y + z

fun fooRelabel(
    x: Int,
    @IntroducedAt("1") y: Int = 1,
): Int = x + y

fun fooDefaultChain(
    @IntroducedAt("1") a: Int = 1,
    @IntroducedAt("2") b: Int = a + 1,
): Int = b

fun fooComparableOrder(
    a: Int = 1,
    @IntroducedAt("1.9") b: Int = 19,
    @IntroducedAt("1.10-beta2") c: Int = 102,
    @IntroducedAt("1.10") d: Int = 110,
): String = "a=$a,b=$b,c=$c,d=$d"

class fooCtor(
    val x: Int,
    @IntroducedAt("1") y: Int = 1,
    @IntroducedAt("2") z: Int = 10,
) {
    val value = x + y + z
}

data class fooData(
    val a: Int,
    val b: String = "B",
)
