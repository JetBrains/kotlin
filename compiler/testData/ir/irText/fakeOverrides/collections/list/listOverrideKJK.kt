// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR

// FILE: Java1.java
import kotlin.collections.AbstractMutableList;

abstract public class Java1 extends AbstractMutableList<Integer> {
    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public void add(int i, Integer integer) {}

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
class B : Java1() {
    override fun removeAt(index: Int): Int {
        return 1
    }
}

fun test(b: B){
    b.size
    b.add(1,1)
    b.add(1)
    b.get(1)
    b.remove(1)
    b.removeAt(1)
    b.removeFirst()
}
