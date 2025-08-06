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

    expectSuccess("direct.v1") { directCall1() }
    expectSuccess("receiver.direct.v1 with context c") { directCall2() }
    expectSuccess("direct.v1") { directCall3() }
    expectSuccess("receiver.direct.v1 with context c") { directCall4() }

    expectSuccess("inline.v1") { inlineCall1() }
    expectSuccess("receiver.inline.v1 with context c") { inlineCall2() }
    expectSuccess("inline.v1") { inlineCall3() }
    expectSuccess("receiver.inline.v1 with context c") { inlineCall4() }

    expectSuccess("lambda.v1") { lambdaCall1() }
    expectSuccess("receiver.lambda.v1 with context c") { lambdaCall2() }
    expectSuccess("lambda.v1") { lambdaCall3() }
    expectSuccess("receiver.lambda.v1 with context c") { lambdaCall4() }
}