// FULL_JDK

import java.util.stream.*

interface A : Collection<String> {
    override fun stream(): Stream<String> = Stream.<!INTERFACE_STATIC_METHOD_CALL_FROM_JAVA6_TARGET_ERROR!>of<!>()
}

fun foo(x: List<String>, y: A) {
    x.stream().filter { it.length > 0 }.collect(Collectors.toList())
    y.stream().filter { it.length > 0 }
}
