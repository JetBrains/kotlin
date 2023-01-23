import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Can not read value from variable 'foo': Variable uses unlinked class symbol '/Foo'")) { fooVariableType() }
    expectFailure(linkage("Can not read value from variable 'bar': Variable uses unlinked class symbol '/Foo' (via class 'Bar')")) { barVariableType() }
    expectFailure(linkage("Constructor 'Foo.<init>' can not be called: No constructor found for symbol '/Foo.<init>'")) { fooInstance() }
    expectFailure(linkage("Constructor 'Bar.<init>' can not be called: Class 'Bar' uses unlinked class symbol '/Foo'")) { barInstance() }
    expectFailure(linkage("Reference to constructor 'Foo.<init>' can not be evaluated: No constructor found for symbol '/Foo.<init>'")) { fooInstance2() }
    expectFailure(linkage("Reference to constructor 'Bar.<init>' can not be evaluated: Class 'Bar' uses unlinked class symbol '/Foo'")) { barInstance2() }
    expectFailure(linkage("Can not read value from variable 'foo': Variable uses unlinked class symbol '/Foo' (via anonymous object)")) { fooAnonymousObject() }
    expectFailure(linkage("Can not read value from variable 'bar': Variable uses unlinked class symbol '/Foo' (via anonymous object)")) { barAnonymousObject() }
}
