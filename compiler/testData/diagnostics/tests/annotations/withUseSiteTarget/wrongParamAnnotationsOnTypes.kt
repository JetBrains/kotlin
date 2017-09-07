// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test1(i: <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@setparam:Suppress<!> Int) {}
fun test2(i: <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@param:Suppress<!> Int) {}
fun test3(i: <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Suppress<!> Int) {}
fun test4(i: <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Suppress<!> Int) {}
fun test5(i: <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@set:Suppress<!> Int) {}

fun test6(): <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@setparam:Suppress<!> Int = TODO()
fun test7(i: (<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@setparam:Suppress<!> Int) -> Unit) {}

fun <!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@setparam:Suppress<!> Int.test8() {}
fun ((<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@setparam:Suppress<!> Int) -> Unit).test9() {}

fun test10(): ((<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@setparam:Suppress<!> Int) -> Unit) = TODO()