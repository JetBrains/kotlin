// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK

// FILE: JavaIterator.java
public class JavaIterator implements java.util.Iterator<String> {
    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public String next() {
        return "";
    }
}

// FILE: JavaIteratorWithRemove.java
public class JavaIteratorWithRemove extends JavaIterator {
    @Override
    public void remove() {
    }
}

// FILE: JavaListIterator.java
public abstract class JavaListIterator implements java.util.ListIterator<String> {
    @Override
    public void set(String s) {
    }
}

// FILE: iterators.kt
class KotlinIterator : Iterator<String> {
    override fun hasNext(): Boolean = false
    override fun next(): String = ""
}

class KotlinMutableIterator : MutableIterator<String> {
    override fun hasNext(): Boolean = false
    override fun next(): String = ""
    override fun remove() {}
}

abstract class KotlinListIterator : ListIterator<String>

abstract class KotlinMutableListIterator : MutableListIterator<String>

class KotlinSubclassOfJava : JavaIteratorWithRemove()
