@file:[JvmName("Util") JvmMultifileClass]
package test

fun foo() {
    privateInThisFile()
    internalInThisFile()
    publicInThisFile()
    internalInOtherFile()
    publicInOtherFile()
}

private fun privateInThisFile() {}

internal fun internalInThisFile() {}

public fun publicInThisFile() {}

// @test/1ThisFileKt.class:
// 1 INVOKESTATIC test/1ThisFileKt.privateInThisFile
// 1 INVOKESTATIC test/1ThisFileKt.internalInThisFile
// 1 INVOKESTATIC test/1ThisFileKt.publicInThisFile
// 1 INVOKESTATIC test/1OtherFileKt.internalInOtherFile
// 1 INVOKESTATIC test/1OtherFileKt.publicInOtherFile