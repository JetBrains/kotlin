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

// @test/Util__ThisFileKt.class:
// 1 INVOKESTATIC test/Util__ThisFileKt.privateInThisFile
// 1 INVOKESTATIC test/Util.internalInThisFile
// 1 INVOKESTATIC test/Util.publicInThisFile
// 1 INVOKESTATIC test/Util.internalInOtherFile
// 1 INVOKESTATIC test/Util.publicInOtherFile