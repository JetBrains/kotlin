// TARGET_BACKEND: JVM_IR

fun f() {}
fun g() = ::f

// This regexp checks that there's only one class annotated with kotlin.Metadata with the actual data (which is in the `d1` field).
// That class is the file facade. The synthetic class for the reference `::f` should be generated with synthetic metadata, i.e.
// which has nothing in the `d1` field.
// 1 @Lkotlin/Metadata;\(.*d1.*\)
