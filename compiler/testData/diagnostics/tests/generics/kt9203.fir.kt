// !WITH_NEW_INFERENCE
// FULL_JDK

import java.util.stream.Collectors
import java.util.stream.IntStream

fun main() {
    val xs = IntStream.range(0, 10).mapToObj { it.toString() }
            .<!INAPPLICABLE_CANDIDATE!>collect<!>(Collectors.toList())
    <!UNRESOLVED_REFERENCE!>xs[0]<!>
}