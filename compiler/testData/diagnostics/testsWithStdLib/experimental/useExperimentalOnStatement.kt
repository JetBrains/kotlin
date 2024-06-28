// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalAPI

@ExperimentalAPI
fun function(): String = ""

fun use(): String {
    @OptIn(ExperimentalAPI::class)
    for (i in 1..2) {
        function()
    }

    @OptIn(ExperimentalAPI::class)
    return function()
}
