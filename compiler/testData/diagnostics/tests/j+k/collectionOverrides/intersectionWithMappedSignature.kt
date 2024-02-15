// FULL_JDK
// SCOPE_DUMP: B1:remove, B2:remove
// FILE: Java1.java
public interface Java1 {
    Boolean remove(Integer element);
}

// FILE: testRemove.kt
import java.util.*

// CONFLICTING_JVM_DECLARATIONS in K1 is only reported with old backend, not with K1.
class <!CONFLICTING_JVM_DECLARATIONS!>B1<!> : ArrayList<Int>(), Java1 {
    <!CONFLICTING_JVM_DECLARATIONS!>override fun remove(element: Int?): Boolean<!> {
        return false
    }
}

// CONFLICTING_JVM_DECLARATIONS in K1 is only reported with old backend, not with K1.
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