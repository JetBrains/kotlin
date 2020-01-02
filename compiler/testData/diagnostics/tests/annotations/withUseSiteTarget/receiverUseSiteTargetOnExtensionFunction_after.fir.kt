// !LANGUAGE: +RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Fancy

fun @receiver:Fancy String.myExtension() { }
val @receiver:Fancy Int.asVal get() = 0

fun ((@receiver:Fancy Int) -> Unit).complexReceiver1() {}
fun ((Int) -> @receiver:Fancy Unit).complexReceiver2() {}