import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(prefixed("var foo can not be read")) { bar() }
    expectFailure(prefixed("var foo can not be read")) { baz() }
    expectFailure(prefixed("var foo can not be read")) { quux() }
    expectFailure(prefixed("var foo can not be read")) { grault() }
    expectFailure(prefixed("var foo can not be read")) { waldo() }
}
