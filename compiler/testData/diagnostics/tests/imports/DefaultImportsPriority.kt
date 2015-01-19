import java.lang.reflect.*
import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.List<!>

fun foo(
        <!UNUSED_PARAMETER!>p1<!>: Array<String> /* should be resolved to kotlin.Array */,
        <!UNUSED_PARAMETER!>p2<!>: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>List<String><!> /* should be resolved to java.util.List */) { }