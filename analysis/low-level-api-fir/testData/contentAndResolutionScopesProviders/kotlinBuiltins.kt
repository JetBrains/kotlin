// ISSUE: KT-86402

// MODULE: LIBRARY
// MODULE_KIND: LibraryBinary
// FILE: Foo.kt
class Foo

// FILE: fake.kotlin_builtins
// SKIP_FILE_NAME_VALIDATION
// LIBRARY_RESOURCE
// This is a mock `.kotlin_builtins` file, not an actual one!

// MODULE: MAIN(LIBRARY)
// FILE: main.kt
class main
