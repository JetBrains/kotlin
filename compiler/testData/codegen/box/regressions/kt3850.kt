// !LANGUAGE: -PrivateInFileEffectiveVisibility

// In light analysis mode, anonymous object type is approximated to the supertype, so `fy` is unresolved.
// IGNORE_LIGHT_ANALYSIS
// JVM_ABI_K1_K2_DIFF: KT-63655

private class One {
    val a1 = arrayOf(
            object { val fy = "text"}
    )
}

fun box() = if (One().a1[0].fy == "text") "OK" else "fail"