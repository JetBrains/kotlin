// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// SKIP_TXT
// FULL_JDK

import java.util.stream.Collectors
import java.util.stream.Stream

fun test(a: Stream<String>) {
    a.<!INAPPLICABLE_CANDIDATE!>collect<!>(Collectors.toList()) <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><MutableList<String>>() }
    // actually the inferred type is platform
    a.<!INAPPLICABLE_CANDIDATE!>collect<!>(Collectors.toList()) <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><List<String?>>() }
}

