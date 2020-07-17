// FILE: test.kt
package localFunInLibrary

fun main(args: Array<String>) {
    customLib.localFunInLibraryCustomLib.localFunInLibraryCustomLibMainFun()
}

// ADDITIONAL_BREAKPOINT: localFunCustomLib.kt / localFunInLibraryCustomLibProperty
// EXPRESSION: localFun()
// RESULT: 1: I

// FILE: localFunCustomLib.kt
package customLib.localFunInLibraryCustomLib

public fun localFunInLibraryCustomLibMainFun() {
    fun localFun() = 1
    val localFunInLibraryCustomLibProperty = 1
}