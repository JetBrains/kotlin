// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class J extends MyList {
    @Override
    public int getSize() {
        return 55;
    }

    @Override
    public int lastIndexOf(String s) {
        return 0;
    }

    @Override
    public int indexOf(String s) {
        return 0;
    }

    @Override
    public boolean contains(String s) {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public String get(int index) {
        return null;
    }

    @Override
    public List<String> subList(int i, int i1) {
        return super.subList(i, i1);
    }

    @Override
    public ListIterator<String> listIterator(int i) {
        return super.listIterator(i);
    }

    @Override
    public ListIterator<String> listIterator() {
        return super.listIterator();
    }
}

// FILE: test.kt

abstract class MyList : List<String>

class ListImpl : J() {
    override val size: Int get() = super.size + 1
}

fun box(): String {
    val impl = ListImpl()
    if (impl.size != 56) return "fail 1"
    if (!impl.contains("abc")) return "fail 2"

    val l: List<String> = impl

    if (l.size != 56) return "fail 3"
    if (!l.contains("abc")) return "fail 4"

    val anyList: List<Any?> = impl as List<Any?>

    if (anyList.size != 56) return "fail 5"
    if (!anyList.contains("abc")) return "fail 6"

    if (anyList.contains(1)) return "fail 7"
    if (anyList.contains(null)) return "fail 8"

    return "OK"
}
