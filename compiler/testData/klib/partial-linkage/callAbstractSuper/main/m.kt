import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Abstract function 'foo' is not implemented in non-abstract class 'BaseClass'")) {
        ChildClass().foo()
    }
}
