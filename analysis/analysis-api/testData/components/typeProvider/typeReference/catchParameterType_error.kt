fun test(flag: Boolean) = runBlocking {
    try {
        foo()
    } catch (e: My<caret>UnknownException) {

    }
}

suspend fun foo() {}
