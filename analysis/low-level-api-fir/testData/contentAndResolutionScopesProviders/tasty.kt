// ISSUE: KT-86402

// MODULE: LIBRARY
// MODULE_KIND: LibraryBinary
// FILE: Foo.kt
class Foo

// FILE: very.tasty
// SKIP_FILE_NAME_VALIDATION
// LIBRARY_RESOURCE
// This is a mock `.tasty` file, not an actual one!

// MODULE: MAIN(LIBRARY)
// FILE: main.kt
class main
