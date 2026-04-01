import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Abstract function 'newFun' is not implemented in non-abstract class 'ExtendsAddedPublicAbstractFun'")) {
        ExtendsAddedPublicAbstractFun().foo()
    }
    expectFailure(linkage("Abstract function 'newFun' is not implemented in non-abstract class 'ExtendsAddedPublicAbstractFun'")) {
        // KT-85018: invocation of `newFun()` via `foo()` causes runtime crash due to missing PL exception
        ExtendsAddedInternalAbstractFun().foo()
    }
}
