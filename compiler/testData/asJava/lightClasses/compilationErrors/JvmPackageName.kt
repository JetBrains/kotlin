// a.b.c.JvmPackageNameKt
// WITH_RUNTIME
@file:JvmPackageName("a.b.c")
package p

fun f() {

}

// FIR_COMPARISON
// SKIP_IDE_TEST