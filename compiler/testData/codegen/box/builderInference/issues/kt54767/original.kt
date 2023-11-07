// ISSUE: KT-54767
// WITH_STDLIB

interface A {
    fun getCallableNames(): Set<String>
}

class B(val declared: A, val supers: List<A>) {
    val callableNamesCached by lazy(LazyThreadSafetyMode.PUBLICATION) { // (1)
        buildSet { // (2)
            addAll(declared.getCallableNames())
            supers.flatMapTo(this) { it.getCallableNames() }
        }
    }
}

fun box(): String {
    val result = B(
        declared = object : A {
            override fun getCallableNames(): Set<String> {
                return setOf("1", "2", "5")
            }
        },
        supers = listOf(
            object : A {
                override fun getCallableNames(): Set<String> {
                    return setOf("2", "3")
                }
            },
            object : A {
                override fun getCallableNames(): Set<String> {
                    return setOf("1", "3", "4")
                }
            }
        )
    ).callableNamesCached
    return result.toString()
}
