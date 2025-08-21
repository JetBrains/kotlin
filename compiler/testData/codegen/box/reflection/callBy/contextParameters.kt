// LANGUAGE: +ContextParameters
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// WITH_REFLECT

import kotlin.reflect.KCallable

class TestClass {
    context(a: (() -> String) -> String) fun superFunWithContextLambda(b: () -> String) = a.invoke(b)
}

fun box(): String {

    val f = TestClass::class.members.single { it.name == "superFunWithContextLambda" } as KCallable<String>
    val contextParam: Function1<Function0<String>, String> = { a: () -> String ->
        a.invoke()
    }


    return f.callBy(
        mapOf(
            f.parameters[0] to TestClass(),
            f.parameters[1] to contextParam,
            f.parameters[2] to { "OK" } as Function0<String>
        )
    )
}