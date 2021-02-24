// a.b.c.ActualTypeAliasCustomJvmPackageNameKt
// WITH_RUNTIME
@file:JvmPackageName("a.b.c")
package p

actual typealias B = List<Int>

// FIR_COMPARISON