// RUN_PIPELINE_TILL: BACKEND
// ISSUES: KT-78881

fun use(a: Any?) {}
fun invokeLater(block: () -> Unit) {}
inline fun invokeInline(block: () -> Unit) { block() }

interface Delegate<T>
fun <T> delegate(value: T): Delegate<T> = null!!
operator fun <T> Delegate<T>.getValue(thisRef: Any?, property: Any?): T = null!!
operator fun <T> Delegate<T>.setValue(thisRef: Any?, property: Any?, value: T) {}

fun test0() {
    var bool by delegate(true)
    use(bool)
}

fun test1() {
    var bool by delegate(true)
    use(bool)
    invokeLater { bool = !bool }
}

fun test2() {
    var bool by delegate(true)
    invokeLater { bool = !bool }
    use(bool)
}

fun test3() {
    var bool by delegate(true)
    invokeLater { bool = !bool }
}

fun test4() {
    var bool by delegate(true)
    invokeLater { bool = false }
}

fun test5() {
    val <!UNUSED_VARIABLE!>bool<!> by delegate(true)
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, inline, interfaceDeclaration,
nullableType, operator, typeParameter */
