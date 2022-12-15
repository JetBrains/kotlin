import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { fooVariableType() }
    expectFailure(linkage("Can not read value from variable bar: Variable bar uses unlinked class symbol /Foo (through class Bar)")) { barVariableType() }
    expectFailure(linkage("Constructor Foo.<init> can not be called: No constructor found for symbol /Foo.<init>")) { fooInstance() }
    expectFailure(linkage("Constructor Bar.<init> can not be called: Constructor Bar.<init> uses unlinked class symbol /Foo (through class Bar)")) { barInstance() }
    expectFailure(linkage("Reference to constructor Foo.<init> can not be evaluated: No constructor found for symbol /Foo.<init>")) { fooInstance2() }
    expectFailure(linkage("Reference to constructor Bar.<init> can not be evaluated: Constructor Bar.<init> uses unlinked class symbol /Foo (through class Bar)")) { barInstance2() }
    expectFailure(linkage("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo (through anonymous object)")) { fooAnonymousObject() }
    expectFailure(linkage("Can not read value from variable bar: Variable bar uses unlinked class symbol /Foo (through anonymous object)")) { barAnonymousObject() }
}
