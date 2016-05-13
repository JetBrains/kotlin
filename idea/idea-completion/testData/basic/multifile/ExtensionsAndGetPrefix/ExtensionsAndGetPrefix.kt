val String.thisFileExtension: Int
    get() = 0

fun f() {
    "".get<caret>
}

// EXIST: thisFileExtension
// EXIST: notImportedExtension
