// TARGET_BACKEND: JVM
// The old backend thinks `toArray(): Array<Int?>` is the same as `toArray(): Array<Any?>`
// IGNORE_BACKEND: JVM
// WITH_RUNTIME
// FILE: MyListWithCustomToArray.java

public abstract class MyListWithCustomToArray<E> extends java.util.AbstractList<E> {
    public Object[] toArray() {
        return new Object[]{null};
    }

    public <T> T[] toArray(T[] a) {
        return a;
    }
}

// FILE: a.kt
class MyList<T>(val list: List<T>): java.util.AbstractList<T>() {
    override fun get(index: Int): T = list[index]

    override val size: Int
        get() = list.size
}

class MyListSubclass<T>(val list: List<T>): MyListWithCustomToArray<T>() {
    override fun get(index: Int): T = list[index]

    override val size: Int
        get() = list.size
}

class MyCollection<T>(val list: List<T>) : Collection<T> by list {
    fun toArray(): Array<Int?> =
        arrayOfNulls<Int>(0)
}

fun box(): String {
    val list1 = MyList(listOf(2, 3, 9)) as java.util.Collection<*>
    list1.toArray().contentToString().let { if (it != "[2, 3, 9]") return "fail 1: $it" }
    list1.toArray(arrayOfNulls<Int>(0)).contentToString().let { if (it != "[2, 3, 9]") return "fail 2: $it" }

    val list2 = MyListSubclass(listOf(2, 3, 9)) as java.util.Collection<*>
    list2.toArray().contentToString().let { if (it != "[null]") return "fail 3: $it" }
    list2.toArray(arrayOfNulls<Int>(1)).contentToString().let { if (it != "[null]") return "fail 4: $it" }

    val list3 = MyCollection(listOf(2, 3, 9)) as java.util.Collection<*>
    list3.toArray().contentToString().let { if (it != "[2, 3, 9]") return "fail 5: $it" }
    list3.toArray(arrayOfNulls<Int>(0)).contentToString().let { if (it != "[2, 3, 9]") return "fail 6: $it" }
    return "OK"
}
