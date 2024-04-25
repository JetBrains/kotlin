// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn

@OptIn(ExperimentalStdlibApi::class)
val list: List<String> = buildList {
    val inner: List<String> = maybe() ?: emptyList()

    addAll(inner)
}

fun maybe(): List<String>? = null