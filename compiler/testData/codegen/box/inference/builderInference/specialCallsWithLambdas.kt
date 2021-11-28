// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

interface FlowCollector<in T> {}

@Suppress("OPT_IN_USAGE_ERROR")
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
        val inv = {}!!
        inv()
    }
}

fun poll71(): Flow<String> {
    return flow {
        val inv = {1f}!!
        inv()
    }
}

fun poll72(): Flow<String> {
    return flow {
        val inv = {{}}!!
        inv()
    }
}

fun poll73(): Flow<String> {
    return flow {
        val inv = ({})!!
        inv
    }
}

fun poll81(): Flow<String> {
    return flow {
        val inv = {} in setOf({})
        inv
    }
}

fun poll82(): Flow<String> {
    return flow {
        val inv = {{}} in setOf({{}})
        inv
    }
}

fun poll83(): Flow<String> {
    return flow {
        val inv = {({})} in setOf({({})})
        inv
    }
}

fun poll85(): Flow<String> {
    return flow {
        val inv = {({"1"})} in setOf({({"1f"})})
        inv
    }
}

fun poll86(): Flow<String> {
    return flow {
        val inv = {({"1"})}!! in setOf({({"1f"})})!!
        inv
    }
}

fun box(): String {
    poll0()
    poll01()
    poll1(true)
    poll11(true)
    poll12(true)
    poll13(true)
    poll2(true)
    poll21(true)
    poll22(true)
    poll23(true)
    poll4()
    poll41()
    poll42()
    poll43()
    poll5()
    poll7()
    poll71()
    poll72()
    poll73()
    poll81()
    poll82()
    poll83()
    poll85()
    poll86()
    return "OK"
}
