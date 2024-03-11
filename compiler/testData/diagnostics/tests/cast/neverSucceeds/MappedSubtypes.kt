// !DIAGNOSTICS: -PLATFORM_CLASS_MAPPED_TO_KOTLIN -UNUSED_PARAMETER -ABSTRACT_MEMBER_NOT_IMPLEMENTED -USELESS_CAST
import java.lang.CharSequence as JCS

class JSub: JCS
class Sub: CharSequence

fun test1(js: JSub) = js <!CAST_NEVER_SUCCEEDS!>as<!> CharSequence
fun test2(js: JSub) = js as JCS

fun test3(s: Sub) = s as CharSequence
fun test4(s: Sub) = s as JCS

fun test5(js: JSub) = js <!CAST_NEVER_SUCCEEDS!>as<!> Sub
fun test6(s: Sub) = s <!CAST_NEVER_SUCCEEDS!>as<!> JSub
