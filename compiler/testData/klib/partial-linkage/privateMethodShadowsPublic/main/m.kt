import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess(2) { ClassWithPrivateShadowingFinal().test() }
    expectSuccess(2) { ClassWithPrivateShadowingOpen().test() }
    expectFailure(linkage("Abstract function 'foo' is not implemented in non-abstract class 'ClassWithPrivateShadowingAbstract'")) {
        ClassWithPrivateShadowingAbstract().test()
    }

    expectSuccess(2) { ClassWithInternalShadowingFinal().test() }
    expectSuccess(2) { ClassWithInternalShadowingOpen().test() }
    /*expectFailure(linkage("Abstract function 'foo' is not implemented in non-abstract class 'ClassWithInternalShadowingAbstract'")) {
        ClassWithInternalShadowingAbstract().test()
    }*/
}
