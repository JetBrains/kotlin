// a.b.c.ActualTypeAliasCustomJvmPackageNameKt
// WITH_STDLIB
@file:JvmPackageName("a.b.c")
package p

actual typealias B = List<Int>

// FIR_COMPARISON
// SKIP_IDE_TEST