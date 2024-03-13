// !LANGUAGE: -PrivateInFileEffectiveVisibility
// IGNORE_BACKEND_K2: ANY
// Reason: unsupported language feature switch OFF

// In light analysis mode, anonymous object type is approximated to the supertype, so `fy` is unresolved.
// IGNORE_LIGHT_ANALYSIS

private class One {
    val a1 = arrayOf(
            object { val fy = "text"}
    )
}

fun box() = if (One().a1[0].fy == "text") "OK" else "fail"