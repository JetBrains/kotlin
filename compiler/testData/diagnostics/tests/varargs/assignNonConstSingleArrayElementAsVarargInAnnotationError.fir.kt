// !LANGUAGE: +ProhibitNonConstValuesAsVarargsInAnnotations, +ProhibitAssigningSingleElementsToVarargsInNamedForm

val nonConstArray = longArrayOf(0)
fun nonConstFun(): LongArray = TODO()

fun nonConstLong(): Long = TODO()

annotation class Anno(vararg val value: Long)

@Anno(value = nonConstArray)
fun foo1() {}

@Anno(value = nonConstFun())
fun foo2() {}

@Anno(value = longArrayOf(nonConstLong()))
fun foo3() {}

@Anno(value = [nonConstLong()])
fun foo4() {}

@Anno(value = *nonConstArray)
fun bar1() {}

@Anno(*nonConstArray)
fun bar2() {}
