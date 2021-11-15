// WITH_STDLIB

fun box(): String {
    val ok = Result.success("OK")
    return ok.getOrNull()!!
}
