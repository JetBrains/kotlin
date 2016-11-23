// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// See https://youtrack.jetbrains.com/issue/KT-14939

val String?.ok: String
    get() = "OK"

fun box() = (null::ok).get()