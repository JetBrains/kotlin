// IGNORE_BACKEND: WASM
class My {
    val my: String = "O"
        get() = { field }() + "K"
}

fun box() = My().my