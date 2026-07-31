// TARGET_BACKEND: JVM
// WITH_REFLECT
// OPT_IN: kotlin.ExperimentalContextParameters
// LANGUAGE: +ContextParameters +CompanionBlocks

import kotlin.reflect.KParameter

@JvmInline
value class Z(val value: String) {
    context(x: Int)
    val value: Int get() = x

    companion {
        val value get() = "static"
    }
}

fun box(): String {
    val withContext = Z::class.members.single { p ->
        p.name == "value" && p.parameters.any { it.kind == KParameter.Kind.CONTEXT }
    }
    if (withContext.call(Z(""), 42) != 42) return "Fail"

    val static = Z::class.members.single { p ->
        p.name == "value" && p.parameters.isEmpty()
    }
    if (static.call() != "static") return "Fail"

    val primary = Z::class.members.single { p ->
        p.name == "value" && p.parameters.any { it.kind == KParameter.Kind.INSTANCE } && p.parameters.none { it.kind == KParameter.Kind.CONTEXT }
    }
    return primary.call(Z("OK")) as String
}
