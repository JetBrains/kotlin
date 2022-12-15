import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Constructor Foo.<init> can not be called: No constructor found for symbol /Foo.<init>")) { bar() }
}
