// WITH_RUNTIME
// SKIP_TXT
// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS -UNUSED_LAMBDA_EXPRESSION -UNCHECKED_CAST -UNUSED_PARAMETER -UNUSED_VARIABLE -EXPERIMENTAL_API_USAGE_ERROR -UNUSED_EXPRESSION

import kotlin.experimental.ExperimentalTypeInference

interface FlowCollector<in T> {}

fun <L> flow(@BuilderInference block: suspend FlowCollector<L>.() -> Unit) = Flow(block)

class Flow<out R>(private val block: suspend FlowCollector<R>.() -> Unit)

fun <R> select(vararg x: R) = x[0]

fun poll0(): Flow<String> {
    return flow {
        val inv = select({}, {})
        inv()
    }
}

fun poll01(): Flow<String> {
    return flow {
        val inv = select({ 1 }, { 1f })
        inv()
    }
}

fun poll1(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { {} } else { {} }
        inv()
    }
}

fun poll11(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { { 1 } } else { { 1f } }
        inv()
    }
}

fun poll12(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) ({ }) else ({ })
        inv()
    }
}

fun poll13(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) ({ 1 }) else ({ 1f })
        inv()
    }
}

fun poll2(flag: Boolean): Flow<String> {
    return flow {
        val inv = when (flag) { true -> {{}} else -> {{}} }
        inv()
    }
}

fun poll21(flag: Boolean): Flow<String> {
    return flow {
        val inv = when (flag) { true -> {{1}} else -> {{1f}} }
        inv()
    }
}

fun poll22(flag: Boolean): Flow<String> {
    return flow {
        val inv = when (flag) { true -> ({}) else -> ({}) }
        inv()
    }
}

fun poll23(flag: Boolean): Flow<String> {
    return flow {
        val inv = when (flag) { true -> ({1}) else -> ({1f}) }
        inv()
    }
}

fun poll4(): Flow<String> {
    return flow {
        val inv = try { {} } finally { {} }
        inv()
    }
}

fun poll41(): Flow<String> {
    return flow {
        val inv = try { {1} } finally { {1f} }
        inv()
    }
}

fun poll42(): Flow<String> {
    return flow {
        val inv = try { ({1}) } finally { ({1f}) }
        inv()
    }
}

fun poll43(): Flow<String> {
    return flow {
        val inv = try { ({}) } finally { ({}) }
        inv()
    }
}

fun poll5(): Flow<String> {
    return flow {
        val inv = try { {1} } catch (e: Exception) { {1f} } finally { {} }
        inv()
    }
}

fun poll7(): Flow<String> {
    return flow {
        val inv = {}<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>
        inv()
    }
}

fun poll71(): Flow<String> {
    return flow {
        val inv = {1f}<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>
        inv()
    }
}

fun poll72(): Flow<String> {
    return flow {
        val inv = {{}}<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>
        inv()
    }
}

fun poll73(): Flow<String> {
    return flow {
        val inv = ({})<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>
        inv
    }
}

fun poll8(): Flow<String> {
    return flow {
        val inv = {<!ARGUMENT_TYPE_MISMATCH!>1<!>} in setOf({1f})
        <!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

fun poll81(): Flow<String> {
    return flow {
        val inv = {} in setOf({})
        <!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

fun poll82(): Flow<String> {
    return flow {
        val inv = {{}} in setOf({{}})
        <!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

fun poll83(): Flow<String> {
    return flow {
        val inv = {({})} in setOf({({})})
        <!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

fun poll84(): Flow<String> {
    return flow {
        val inv = {<!ARGUMENT_TYPE_MISMATCH!>{1}<!>} in setOf({{1f}})
        <!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

fun poll85(): Flow<String> {
    return flow {
        val inv = {({"1"})} in setOf({({"1f"})})
        <!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

fun poll86(): Flow<String> {
    return flow {
        val inv = {({"1"})}<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!> in setOf({({"1f"})}<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>)
        <!UNRESOLVED_REFERENCE!>inv<!>()
    }
}
