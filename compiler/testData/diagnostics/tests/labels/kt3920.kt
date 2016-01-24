// !DIAGNOSTICS: -UNUSED_VARIABLE
//KT-3920 Labeling information is lost when passing through some expressions

fun test() {
    run f@{
        val x = if (1 > 2) return@f 1 else 2
        2
    }
}
