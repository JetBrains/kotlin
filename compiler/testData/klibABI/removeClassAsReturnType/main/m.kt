import abitestutils.abiTest

fun box() = abiTest {
    val d = D()
    expectFailure(prefixed("function expF can not be called")) { d.barF() }
    expectSuccess { d.fooF() }
    expectFailure(prefixed("property accessor expP1.<get-expP1> can not be called")) { d.barP1 }
    expectSuccess { d.fooP1 }
    expectFailure(prefixed("property accessor expP2.<get-expP2> can not be called")) { D2().barP2 }
    expectFailure(prefixed("function foo can not be called")) { bar() }
    expectFailure(prefixed("function foo can not be called")) { baz() }
    expectFailure(prefixed("function foo can not be called")) { quux() }
    expectFailure(prefixed("function foo can not be called")) { grault() }
    expectFailure(prefixed("function foo can not be called")) { waldo() }
}
