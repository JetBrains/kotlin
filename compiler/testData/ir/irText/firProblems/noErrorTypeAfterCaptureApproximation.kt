// FIR_IDENTICAL
class Inv1<X>
class Inv2<Y>

internal fun <F> useSelectOptions(
    options: Inv1<out F>,
): Inv1<out Inv2<F>> =
    myRun {
        getSelectOptions(options)
    }

private fun <G> getSelectOptions(
    options: Inv1<out G>,
): Inv1<out Inv2<G>> = TODO()

fun <T> myRun(
    callback: () -> T,
): T = TODO()