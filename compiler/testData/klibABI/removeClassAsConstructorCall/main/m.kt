import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(skipHashes("Constructor Foo.<init> can not be called: No constructor found for symbol /Foo.<init>")) { bar() }
}
