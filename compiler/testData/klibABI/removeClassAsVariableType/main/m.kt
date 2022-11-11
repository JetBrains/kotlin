import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(skipHashes("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { barRead() }
    expectFailure(skipHashes("Can not write value to variable foo: Variable foo uses unlinked class symbol /Foo")) { barWrite() }
    expectFailure(skipHashes("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { bazRead() }
    expectFailure(skipHashes("Can not write value to variable foo: Variable foo uses unlinked class symbol /Foo")) { bazWrite() }
    expectFailure(skipHashes("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { quuxRead() }
    expectFailure(skipHashes("Can not write value to variable foo: Variable foo uses unlinked class symbol /Foo")) { quuxWrite() }
    expectFailure(skipHashes("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { graultRead() }
    expectFailure(skipHashes("Can not write value to variable foo: Variable foo uses unlinked class symbol /Foo")) { graultWrite() }
    expectFailure(skipHashes("Can not read value from variable foo: Variable foo uses unlinked class symbol /Foo")) { waldoRead() }
    expectFailure(skipHashes("Can not write value to variable foo: Variable foo uses unlinked class symbol /Foo")) { waldoWrite() }
}
