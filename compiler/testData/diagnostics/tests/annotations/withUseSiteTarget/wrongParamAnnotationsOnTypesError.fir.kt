// !LANGUAGE: +RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test1(i: @setparam:Suppress Int) {}
fun test2(i: @param:Suppress Int) {}
fun test3(i: @receiver:Suppress Int) {}

fun test4(): @setparam:Suppress Int = TODO()
fun test5(i: (@setparam:Suppress Int) -> Unit) {}

fun ((@setparam:Suppress Int) -> Unit).test6() {}

fun test7(): ((@setparam:Suppress Int) -> Unit) = TODO()