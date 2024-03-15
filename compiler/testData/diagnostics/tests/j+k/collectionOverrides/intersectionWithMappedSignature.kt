// FIR_IDENTICAL
// FULL_JDK
// SCOPE_DUMP: B1:remove, B2:remove
// FILE: Java1.java
public interface Java1 {
    Boolean remove(Integer element);
}

// FILE: testRemove.kt
import java.util.*

class B1 : ArrayList<Int>(), Java1 {
    override fun remove(element: Int?): Boolean {
        return false
    }
}

class B2 : ArrayList<Int>(), Java1 {
}

// FILE: Java2.java
public interface Java2 {
    Character get(Integer i);
}

// FILE: MyString.java
public abstract class MyString implements CharSequence {
    @Override
    public char charAt(int i) {
        return 'c';
    }
}

// FILE: testRenamed.kt
abstract class B3 : MyString(), Java2 {
    override fun get(i: Int?): Char? = null
}
