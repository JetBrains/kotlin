// !WITH_NEW_INFERENCE
// FILE: J.java
import java.util.*;

public class J {
    public static String s = null;
    public static Map<String, String> m = null;
}

// FILE: k.kt

val testImplicitExclExcl1: String = J.s
val testImplicitExclExcl2: String? = J.s

val testImplicitExclExcl3: String = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>J.m[""]<!>
val testImplicitExclExcl4: String? = J.m[""]

val testExclExcl1: String = J.s!!
val testExclExcl2: String? = J.s!!

val testExclExcl3: String = J.m[""]!!
val testExclExcl4: String? = J.m[""]!!

val testSafeCall1: String = <!INITIALIZER_TYPE_MISMATCH!>J.s?.let { it }<!>
val testSafeCall2: String? = J.s?.let { it }

val testSafeCall3: String = <!INITIALIZER_TYPE_MISMATCH!>J.m[""]?.let { it }<!>
val testSafeCall4: String? = J.m[""]?.let { it.toString() }

val testIf1: String = if (true) J.s else J.s
val testIf2: String? = if (true) J.s else J.s

val testIf3: String = <!INITIALIZER_TYPE_MISMATCH!>if (true) J.m[""] else J.m[""]<!>
val testIf4: String? = if (true) J.m[""] else J.m[""]

val testWhen1: String = when { else -> J.s }
val testWhen2: String? = when { else -> J.s }

val testWhen3: String = <!INITIALIZER_TYPE_MISMATCH!>when { else -> J.m[""] }<!>
val testWhen4: String? = when { else -> J.m[""] }
