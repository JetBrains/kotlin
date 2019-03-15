// !LANGUAGE: +RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Fancy

fun @receiver:Fancy String.myExtension() { }
val @receiver:Fancy Int.asVal get() = 0

fun ((<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Fancy<!> Int) -> Unit).complexReceiver1() {}
fun ((Int) -> <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Fancy<!> Unit).complexReceiver2() {}