// FILE: otherFile.kt

@file:[JvmName("Util") JvmMultifileClass]
package test

public fun publicInOtherFile() {}

// FILE: thisFile.kt

@file:[JvmName("Util") JvmMultifileClass]
package test

inline fun foo(body: () -> Unit) {
    publicInThisFile()
    publicInOtherFile()
    body()
}

public fun publicInThisFile() {}

fun bar() {
    foo {}
}

// @test/Util__ThisFileKt.class:
// 2 INVOKESTATIC test/Util.publicInThisFile
// 2 INVOKESTATIC test/Util.publicInOtherFile
