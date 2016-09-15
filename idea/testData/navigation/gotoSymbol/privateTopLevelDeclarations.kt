package foo.bar

private class test

private fun Int.test() = 1

private fun test() = 2

private val Int.test: Int
    get() = 3

private val test = 4

// SEARCH_TEXT: test
// REF: (for Int in privateTopLevelDeclarations.kt in foo.bar).test
// REF: (for Int in privateTopLevelDeclarations.kt in foo.bar).test()
// REF: (privateTopLevelDeclarations.kt in foo.bar).test
// REF: (privateTopLevelDeclarations.kt in foo.bar).test
// REF: (privateTopLevelDeclarations.kt in foo.bar).test()