// TARGET_BACKEND: JVM
// FULL_JDK
// ISSUE: KT-65555
// JVM_ABI_K1_K2_DIFF: KT-63828

interface MyCollection<E> : Collection<E>
interface MyList<E> : MyCollection<E>, List<E>
interface MyMutableList<E> : MyList<E>, MutableList<E>

class A(val delegate: ArrayList<String>) : MyMutableList<String>, MutableList<String> by delegate

fun box(): String {
    val delegate = ArrayList<String>()
    delegate.add("OK")
    return A(delegate).get(0)
}