// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// WITH_STDLIB

// FILE: A.java
public class A {
    private String[] myStrings = null;

    public A withStrings(String... args) {
        myStrings = args;
        return this;
    }

    public String[] getStrings() {
        return myStrings;
    }
}

// FILE: B.java
public class B {
    private String[] myStrings = null;

    public B withStrings(String... args) {
        myStrings = args;
        return this;
    }

    public String[] getStrings() {
        return myStrings;
    }
}

// FILE: main.kt

class ListsMerger<SRC, TRG, VAL>(
    private val sourceGetter: (SRC) -> List<VAL>?,
    private val targetGetter: (TRG) -> List<VAL>?,
    private val setter: (TRG, List<VAL>) -> Any
) {
    fun mergeLists(source: SRC, target: TRG) {
        val fromSource = sourceGetter(source)
        val existing = targetGetter(target)
        val result: List<VAL> = (fromSource ?: emptyList()) + (existing ?: emptyList())

        setter(target, result)
    }
}

inline fun <SRC, TRG, reified VAL> mergeArrays(
    source: SRC,
    target: TRG,
    crossinline sourceGetter: (SRC) -> Array<VAL>?,
    crossinline targetGetter: (TRG) -> Array<VAL>?,
    crossinline setter: (TRG, Array<VAL>) -> Any
) {
    ListsMerger<SRC, TRG, VAL>(
        sourceGetter = { sourceGetter(it)?.toList() },
        targetGetter = { targetGetter(it)?.toList() },
        setter = { trg, list -> setter(trg, list.toTypedArray()) }
    ).mergeLists(source, target)
}

fun box(): String {
    val a = A().withStrings("a", "b", "c")
    val b = B().withStrings("d", "e")

    mergeArrays(a, b, A::getStrings, B::getStrings, B::withStrings)

    val merged = b.getStrings().contentToString()

    return if (merged == "[a, b, c, d, e]") "OK" else "FAIL $merged"
}
