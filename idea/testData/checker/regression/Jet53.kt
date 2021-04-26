// FIR_IDENTICAL
import java.util.Collections

fun <T> checkSubtype(t: T) = t

val ab = checkSubtype<List<Int>?>(Collections.emptyList<Int>())
