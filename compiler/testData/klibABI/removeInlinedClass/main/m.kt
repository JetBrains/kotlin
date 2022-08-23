import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(prefixed("val foo can not be read")) { fooVariableType() }
    expectFailure(prefixed("val bar can not be read")) { barVariableType() }
    expectFailure(prefixed("constructor Foo.<init> can not be called")) { fooInstance() }
    expectFailure(prefixed("constructor Bar.<init> can not be called")) { barInstance() }
    expectFailure(prefixed("reference to constructor Foo.<init> can not be evaluated")) { fooInstance2() }
    expectFailure(prefixed("reference to constructor Bar.<init> can not be evaluated")) { barInstance2() }
    expectFailure(prefixed("val foo can not be read")) { fooAnonymousObject() }
}
