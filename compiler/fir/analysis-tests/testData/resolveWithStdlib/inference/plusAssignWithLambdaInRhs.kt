// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-39005
// DUMP_CFG

fun test() {
    val list: MutableList<(String) -> String> = null!!
    list += { it }
}

class A<T>(private val executor: ((T) -> Unit) -> Unit)

fun <T> postpone(computation: () -> T): A<T> {
    val queue = mutableListOf<() -> Unit>()

    return A { resolve ->
        queue += {
            resolve(computation())
        }
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, functionalType, lambdaLiteral,
localProperty, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
