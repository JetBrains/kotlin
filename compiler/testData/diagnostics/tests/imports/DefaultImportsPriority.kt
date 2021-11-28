// FIR_IDENTICAL
// WITH_EXTENDED_CHECKERS

import java.lang.reflect.*
import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.List<!>

fun foo(
        p1: Array<String> /* should be resolved to kotlin.Array */,
        p2: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>List<String><!> /* should be resolved to java.util.List */) { }
