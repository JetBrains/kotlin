// FIR_IDENTICAL
// FULL_JDK

import java.util.stream.Collectors
import java.util.stream.IntStream

fun main() {
    val xs = IntStream.range(0, 10).mapToObj { it.toString() }
            .collect(Collectors.toList())
    xs[0]
}
