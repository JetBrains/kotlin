// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test1(i: <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE!>@setparam:Suppress<!> Int) {}
fun test2(i: <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE!>@param:Suppress<!> Int) {}
fun test3(i: <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE!>@receiver:Suppress<!> Int) {}

fun test4(): <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE!>@setparam:Suppress<!> Int = TODO()
fun test5(i: (<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE!>@setparam:Suppress<!> Int) -> Unit) {}

fun ((<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE!>@setparam:Suppress<!> Int) -> Unit).test6() {}

fun test7(): ((<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE!>@setparam:Suppress<!> Int) -> Unit) = TODO()