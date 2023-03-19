fun main(args: Array<<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Anno<!> String>) {}

annotation class Anno

fun Int.train(args: Array<<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Anno<!> String>) {}

fun Int.plane(<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Anno<!> args: Array<String>) {}

fun vein(args: Array<<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:Anno<!> String>) {}

fun rain(args: Array<<!WRONG_ANNOTATION_TARGET!>@Anno<!> String>) {}

fun <!WRONG_ANNOTATION_TARGET!>@Anno<!> Int.strain() {}

fun @receiver:Anno Int.drain() {}

fun <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@file:Anno<!> Int.brain() {}

fun (<!WRONG_ANNOTATION_TARGET!>@Anno<!> Int).crane() {}

@Target(AnnotationTarget.FILE)
annotation class Anno2

fun <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@receiver:Anno2<!> Int.pain() {}
