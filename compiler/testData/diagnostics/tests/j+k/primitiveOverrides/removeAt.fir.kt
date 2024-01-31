// WITH_STDLIB
// SCOPE_DUMP: A:removeAt
// ISSUE: KT-65410

// FILE: Java1.java
import kotlin.collections.AbstractMutableList;

public class Java1 extends AbstractMutableList<Integer> {
    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public void add(int i, Integer integer) {}

    @Override
    public Integer removeAt(int i) {
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
<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED("Class 'A'; removeAt")!>class A<!> : Java1()
