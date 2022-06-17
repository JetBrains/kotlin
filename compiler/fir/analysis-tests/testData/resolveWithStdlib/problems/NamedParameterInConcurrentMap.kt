// FULL_JDK

import java.util.concurrent.ConcurrentHashMap

fun main() {
    val map = ConcurrentHashMap<String, String>()
    map.put(
        <!NAMED_PARAMETER_NOT_FOUND!>key<!> = "key",
        <!NAMED_PARAMETER_NOT_FOUND!>value<!> = "value"
    <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>)<!>
}
