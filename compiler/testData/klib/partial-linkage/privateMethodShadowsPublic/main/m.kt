import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess(2) { ClassWithPrivateShadowingFinal().test() }
    expectSuccess(2) { ClassWithPrivateShadowingOpen().test() }
    expectFailure(linkage("Abstract function 'foo' is not implemented in non-abstract class 'ClassWithPrivateShadowingAbstract'")) {
        ClassWithPrivateShadowingAbstract().test()
    }

    expectSuccess(2) { ClassWithInternalShadowingFinal().test() }
    expectSuccess(2) { ClassWithInternalShadowingOpen().test() }
    expectFailure(linkage("Abstract function 'foo' is not implemented in non-abstract class 'ClassWithInternalShadowingAbstract'")) {
        ClassWithInternalShadowingAbstract().test()
    }

    expectSuccess(2) { ClassWithInternalPAShadowingFinal().test() }
    expectSuccess(3) { ClassWithInternalPAShadowingOpen().test() }
    expectSuccess(3) { ClassWithInternalPAShadowingAbstract().test() }

    // Even though not permitted by language, and as oppsed to `private`, `protected` member does override `public` member.
    expectSuccess(2) { ClassWithProtectedShadowingFinal().test() }
    expectSuccess(3) { ClassWithProtectedShadowingOpen().test() }
    expectSuccess(3) { ClassWithProtectedShadowingAbstract().test() }
}
