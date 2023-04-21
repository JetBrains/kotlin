// FIR_IDENTICAL
// ISSUE: KT-57543

class KotlinVal<T>(initializer: () -> T) {
    operator fun getValue(instance: Any?, metadata: Any?): T  = TODO()
}

class A(
    myType: (() -> Int)?
) {
    val arguments: A by KotlinVal {
        A(select(null, fun(): Int { return 1 }))
    }
}

fun <E> select(e: E, f: E): E = TODO()
