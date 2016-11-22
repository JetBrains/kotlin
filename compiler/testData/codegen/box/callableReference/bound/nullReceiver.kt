val String?.ok: String
    get() = "OK"

fun box() = (null::ok).get()