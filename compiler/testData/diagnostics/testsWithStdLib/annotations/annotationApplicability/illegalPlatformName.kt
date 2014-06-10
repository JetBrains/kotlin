// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.platform.*

[<!ILLEGAL_PLATFORM_NAME!>platformName("")<!>]
fun foo(a: Any) {}

[<!ILLEGAL_PLATFORM_NAME!>platformName(".")<!>]
fun foo() {}

[<!ILLEGAL_PLATFORM_NAME!>platformName("/")<!>]
fun fooSlash() {}

[<!ILLEGAL_PLATFORM_NAME!>platformName("<")<!>]
fun fooLT() {}