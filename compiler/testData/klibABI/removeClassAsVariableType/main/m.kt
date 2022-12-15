import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { barRead() }
    expectFailure(linkage("Can not write value to variable foo: Variable foo uses unlinked class symbol /Foo")) { barWrite() }
    expectFailure(linkage("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { bazRead() }
    expectFailure(linkage("Can not write value to variable foo: Variable foo uses unlinked class symbol /Foo")) { bazWrite() }
    expectFailure(linkage("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { quuxRead() }
    expectFailure(linkage("Can not write value to variable foo: Variable foo uses unlinked class symbol /Foo")) { quuxWrite() }
    expectFailure(linkage("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { graultRead() }
    expectFailure(linkage("Can not write value to variable foo: Variable foo uses unlinked class symbol /Foo")) { graultWrite() }
    expectFailure(linkage("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { waldoRead() }
    expectFailure(linkage("Can not write value to variable foo: Variable foo uses unlinked class symbol /Foo")) { waldoWrite() }
}
