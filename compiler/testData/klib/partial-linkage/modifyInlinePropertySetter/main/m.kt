import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("access.v2") {
        inlineProperty = "access"
        inlineProperty
    }
    expectSuccess("receiver.access.v2 with context c") {
        with("c") {
            val s = "receiver"
            s.inlineExtensionProperty = "access"
            s.inlineExtensionProperty
        }
    }
    expectSuccess("access.v2") {
        C().run {
            inlineClassProperty = "access"
            inlineClassProperty
        }
    }
    expectSuccess("receiver.access.v2 with context c") {
        C().run {
            with("c") {
                val s = "receiver"
                s.inlineClassExtensionProperty = "access"
                s.inlineClassExtensionProperty
            }
        }
    }

    expectSuccess("directSetterValue.v1") { directCall1() }
    expectSuccess("directReceiver.directSetterValue.v1 with context directContext") { directCall2() }
    expectSuccess("directSetterValue.v1") { directCall3() }
    expectSuccess("directClassReceiver.directSetterValue.v1 with context directClassContext") { directCall4() }

    expectSuccess("inlineSetterValue.v1") { directCall5() }
    expectSuccess("inlineReceiver.inlineSetterValue.v1 with context inlineContext") { directCall6() }
    expectSuccess("inlineSetterValue.v1") { directCall7() }
    expectSuccess("inlineClassReceiver.inlineSetterValue.v1 with context inlineClassContext") { directCall8() }

    expectSuccess("lambdaSetterValue.v1") { directCall9() }
    expectSuccess("lambdaReceiver.lambdaSetterValue.v1 with context lambdaContext") { directCall10() }
    expectSuccess("lambdaSetterValue.v1") { directCall11() }
    expectSuccess("lambdaClassReceiver.lambdaSetterValue.v1 with context lambdaClassContext") { directCall12() }
}