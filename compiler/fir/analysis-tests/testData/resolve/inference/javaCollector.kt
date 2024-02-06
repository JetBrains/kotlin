// ISSUE: KT-50134
// WITH_STDLIB
// FULL_JDK

import java.util.stream.Collectors

fun foo(){
    listOf("").stream().collect(
        <!ARGUMENT_TYPE_MISMATCH, NEW_INFERENCE_ERROR!>Collectors.groupingBy(
            { it },
            Collectors.collectingAndThen(
                Collectors.counting<String>(),
                Long::toInt
            )
        )<!>
    )
}
