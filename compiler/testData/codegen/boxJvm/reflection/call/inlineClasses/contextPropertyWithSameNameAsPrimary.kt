// TARGET_BACKEND: JVM
// WITH_REFLECT
// OPT_IN: kotlin.ExperimentalContextParameters
// LANGUAGE: +ContextParameters

import kotlin.reflect.KParameter

@JvmInline
value class Z(val value: String) {
    context(x: Int)
    val value: Int get() = x
}

fun box(): String {
    val withContext = Z::class.members.single { p ->
        p.name == "value" && p.parameters.filter { it.kind == KParameter.Kind.CONTEXT }.isNotEmpty()
    }
    if (withContext.call(Z(""), 42) != 42) return "Fail"

    val primary = Z::class.members.single { p ->
        p.name == "value" && p.parameters.filter { it.kind == KParameter.Kind.CONTEXT }.isEmpty()
    }
    return primary.call(Z("OK")) as String
}
