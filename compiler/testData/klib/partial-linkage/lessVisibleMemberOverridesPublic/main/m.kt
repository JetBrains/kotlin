import abitestutils.abiTest

fun box() = abiTest {
    if (testMode.isJs) {
        // In JS all methods are open, so even Kotlin `final` memeber may be accitentally overridden by a one with the same siganture.
        expectSuccess(3) { ClassWithPublicShadowingFinal().test() }
    } else {
        expectSuccess(2) { ClassWithPublicShadowingFinal().test() }
    }
    expectSuccess(3) { ClassWithPublicShadowingOpen().test() }
    expectSuccess(3) { ClassWithPublicShadowingAbstract().test() }

    if (testMode.isJs) {
        // In JS all methods are open, so even Kotlin `final` memeber may be accitentally overridden by a one with the same siganture.
        expectSuccess(3) { ClassWithProtectedShadowingFinal().test() }
    } else {
        expectSuccess(2) { ClassWithProtectedShadowingFinal().test() }
    }
    expectSuccess(3) { ClassWithProtectedShadowingOpen().test() }
    expectSuccess(3) { ClassWithProtectedShadowingAbstract().test() }

    if (testMode.isJs) {
        // In JS all methods are open, so even Kotlin `final` memeber may be accitentally overridden by a one with the same siganture.
        expectSuccess(3) { ClassWithInternalPAShadowingFinal().test() }
    } else {
        expectSuccess(2) { ClassWithInternalPAShadowingFinal().test() }
    }
    expectSuccess(3) { ClassWithInternalPAShadowingOpen().test() }
    expectSuccess(3) { ClassWithInternalPAShadowingAbstract().test() }

    if (testMode.isJs) {
        // In JS all methods are open, so even Kotlin `final` memeber may be accitentally overridden by a one with the same siganture.
        expectSuccess(3) { ClassWithInternalShadowingFinal().test() }
    } else {
        expectSuccess(2) { ClassWithInternalShadowingFinal().test() }
    }
    expectSuccess(3) { ClassWithInternalShadowingOpen().test() }
    expectSuccess(3) { ClassWithInternalShadowingAbstract().test() }

    expectSuccess(2) { ClassWithPrivateShadowingFinal().test() }
    expectSuccess(2) { ClassWithPrivateShadowingOpen().test() }
    expectFailure(linkage("Abstract function 'foo' is not implemented in non-abstract class 'ClassWithPrivateShadowingAbstract'")) {
        ClassWithPrivateShadowingAbstract().test()
    }
}
