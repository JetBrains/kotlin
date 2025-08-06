import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("inlineFunction.v2") { inlineFunction() }
    expectSuccess("inlineFunctionWithParam.v2: default.v2") { inlineFunctionWithParam() }
    expectSuccess("receiver.inlineExtensionFunction.v2 with context c") { with("c") { "receiver".inlineExtensionFunction() } }
    expectSuccess("inlineClassFunction.v2") { C().inlineClassFunction() }
    expectSuccess("inlineClassFunctionWithParam.v2: default.v2") { C().inlineClassFunctionWithParam() }
    expectSuccess("receiver.inlineClassExtensionFunction.v2 with context c") { C().run { with("c") { "receiver".inlineClassExtensionFunction() } } }


    expectSuccess("inlineFunction.v1") { directCall1() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { directCall2() }
    expectSuccess("inlineFunctionWithParam.v1: custom") { directCall3() }
    expectSuccess("receiver.inlineExtensionFunction.v1 with context c") { directCall4() }
    expectSuccess("inlineClassFunction.v1") { directCall5() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { directCall6() }
    expectSuccess("inlineClassFunctionWithParam.v1: custom") { directCall7() }
    expectSuccess("receiver.inlineClassExtensionFunction.v1 with context c") { directCall8() }

    expectSuccess("inlineFunction.v1") { inlineCall1() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { inlineCall2() }
    expectSuccess("inlineFunctionWithParam.v1: custom") { inlineCall3() }
    expectSuccess("receiver.inlineExtensionFunction.v1 with context c") { inlineCall4() }
    expectSuccess("inlineClassFunction.v1") { inlineCall5() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { inlineCall6() }
    expectSuccess("inlineClassFunctionWithParam.v1: custom") { inlineCall7() }
    expectSuccess("receiver.inlineClassExtensionFunction.v1 with context c") { inlineCall8() }

    expectSuccess("inlineFunction.v1") { lambdaCall1() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { lambdaCall2() }
    expectSuccess("inlineFunctionWithParam.v1: custom") { lambdaCall3() }
    expectSuccess("receiver.inlineExtensionFunction.v1 with context c") { lambdaCall4() }
    expectSuccess("inlineClassFunction.v1") { lambdaCall5() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { lambdaCall6() }
    expectSuccess("inlineClassFunctionWithParam.v1: custom") { lambdaCall7() }
    expectSuccess("receiver.inlineClassExtensionFunction.v1 with context c") { lambdaCall8() }

    expectSuccess("inlineFunction.v1") { defaultParamFunction1() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { defaultParamFunction2() }
    expectSuccess("inlineClassFunction.v1") { defaultParamFunction3() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { defaultParamFunction4() }
}