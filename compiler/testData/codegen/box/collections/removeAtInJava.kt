// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// ISSUE: KT-65410

// FILE: Java1.java
import kotlin.collections.AbstractMutableList;

public class Java1 extends AbstractMutableList<Integer> {
    int size = 0;

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void add(int i, Integer integer) {
        size++;
    }

    @Override
    public Integer removeAt(int i) {
        size--;
        return null;
    }

    @Override
    public Integer get(int index) {
        return null;
    }

    @Override
    public Integer set(int i, Integer integer) {
        return null;
    }
}

// FILE: 1.kt
class A : Java1()

fun box(): String {
    val list = A()
    list.add(1)
    list.removeAt(0)
    if (list.isNotEmpty()) return "Not empty"
    if (list.size != 0) return "Size = ${list.size}"
    return "OK"
}
