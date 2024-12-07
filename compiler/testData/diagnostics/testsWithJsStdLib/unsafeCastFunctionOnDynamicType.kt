// FIR_IDENTICAL

fun String.toDouble(): Double = (+(this.asDynamic())).unsafeCast<Double>().also {
    if (it.isNaN() && !this.isNaN() || it == 0.0 && this.isBlank())
        TODO()
}

fun String.isNaN(): Boolean = when (this.lowercase()) {
    "nan", "+nan", "-nan" -> true
    else -> false
}