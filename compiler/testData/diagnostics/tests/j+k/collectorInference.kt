// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// SKIP_TXT
// FULL_JDK

import java.util.stream.Collectors
import java.util.stream.Stream

fun test(a: Stream<String>) {
    a.collect(Collectors.toList()) checkType { _<MutableList<String>>() }
    // actually the inferred type is platform
    a.collect(Collectors.toList()) checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><List<String?>>() }
}

