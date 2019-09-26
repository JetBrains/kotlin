package localFunInLibrary

fun main(args: Array<String>) {
    customLib.localFunInLibraryCustomLib.localFunInLibraryCustomLibMainFun()
}

// ADDITIONAL_BREAKPOINT: localFunCustomLib.kt:localFunInLibraryCustomLibProperty
// EXPRESSION: localFun()
// RESULT: 1: I