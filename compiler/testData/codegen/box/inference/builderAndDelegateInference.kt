// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// ISSUE: KT-54767

interface A {
    fun getCallableNames(): Set<String>
}

class B(val declared: A, val supers: List<A>) {
    private val callableNamesCached by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSet {
            addAll(declared.getCallableNames())
            supers.flatMapTo(this) { it.getCallableNames() }
        }
    }
}

fun box() = "OK"
