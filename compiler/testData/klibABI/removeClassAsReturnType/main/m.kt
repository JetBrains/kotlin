import abitestutils.abiTest

fun box() = abiTest {
    val d = D()
    expectFailure(linkage("Function 'expF' can not be called: Function uses unlinked class symbol '/E'")) { d.barF() }
    expectSuccess { d.fooF() }
    expectFailure(linkage("Property accessor 'expP1.<get-expP1>' can not be called: Property accessor uses unlinked class symbol '/E'")) { d.barP1 }
    expectSuccess { d.fooP1 }
    expectFailure(linkage("Property accessor 'expP2.<get-expP2>' can not be called: Property accessor uses unlinked class symbol '/E'")) { D2().barP2 }
    expectFailure(linkage("Function 'foo' can not be called: Function uses unlinked class symbol '/E'")) { bar() }
    expectFailure(linkage("Function 'foo' can not be called: Function uses unlinked class symbol '/E'")) { baz() }
    expectFailure(linkage("Function 'foo' can not be called: Function uses unlinked class symbol '/E'")) { quux() }
    expectFailure(linkage("Function 'foo' can not be called: Function uses unlinked class symbol '/E'")) { grault() }
    expectFailure(linkage("Function 'foo' can not be called: Function uses unlinked class symbol '/E'")) { waldo() }
}
