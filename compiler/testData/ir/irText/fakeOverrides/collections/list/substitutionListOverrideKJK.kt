// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR

// FILE: Java1.java
import kotlin.collections.AbstractMutableList;

abstract public class Java1<T> extends AbstractMutableList<T> {
    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public void add(int i, T t) {}

    @Override
    public T get(int index) {
        return null;
    }

    @Override
    public T set(int i, T t) {
        return null;
    }
}

// FILE: 1.kt

abstract class A<T> : Java1<T>()

class B<T> : Java1<T>() {
    override fun removeAt(index: Int): T {
        return null!!
    }
}
fun test(a: A<Int?>, b: B<Int>){
    a.size
    a.add(1,1)
    a.add(1)
    a.add(null)
    a.get(1)
    a.remove(1)
    a.remove(null)
    a.removeAt(1)

    b.size
    b.add(1,1)
    b.add(1)
    b.get(1)
    b.remove(1)
    b.removeAt(1)
}