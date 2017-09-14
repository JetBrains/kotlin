// !CHECK_TYPE
// SKIP_TXT
// FULL_JDK

import java.util.stream.Collectors
import java.util.stream.Stream

fun test(a: Stream<String>) {
    a.collect(Collectors.toList()) checkType { _<MutableList<String>>() }
    // actually the inferred type is platform
    a.collect(Collectors.toList()) checkType { _<List<String?>>() }
}

