import abitestutils.abiTest

fun box() = abiTest {
    val d = D()
    expectFailure(skipHashes("Function expF can not be called: Function expF uses unlinked class symbol /E")) { d.barF() }
    expectSuccess { d.fooF() }
    expectFailure(skipHashes("Property accessor expP1.<get-expP1> can not be called: Property accessor expP1.<get-expP1> uses unlinked class symbol /E")) { d.barP1 }
    expectSuccess { d.fooP1 }
    expectFailure(skipHashes("Property accessor expP2.<get-expP2> can not be called: Property accessor expP2.<get-expP2> uses unlinked class symbol /E")) { D2().barP2 }
    expectFailure(skipHashes("Function foo can not be called: Function foo uses unlinked class symbol /E")) { bar() }
    expectFailure(skipHashes("Function foo can not be called: Function foo uses unlinked class symbol /E")) { baz() }
    expectFailure(skipHashes("Function foo can not be called: Function foo uses unlinked class symbol /E")) { quux() }
    expectFailure(skipHashes("Function foo can not be called: Function foo uses unlinked class symbol /E")) { grault() }
    expectFailure(skipHashes("Function foo can not be called: Function foo uses unlinked class symbol /E")) { waldo() }
}
