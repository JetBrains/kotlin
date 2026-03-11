// TARGET_BACKEND: JVM_IR
// ^Old backend reports CONFLICTING_JVM_DECLARATIONS on C1, K1 with IR is fine.
// FULL_JDK
// FILE: Remove.java
public interface Remove {
    Boolean remove(Integer element);
}

// FILE: GetBoxed.java
public interface GetBoxed {
    Character get(Integer i);
}

// FILE: GetPrimitive.java
public interface GetPrimitive {
    Character get(int i);
}

// FILE: MyString.java
public abstract class MyString implements CharSequence {
    @Override public char charAt(int i) { return 'j'; }

    @Override public int length() { return 0; }
    @Override public CharSequence subSequence(int start, int end) { return null; }
}

// FILE: box.kt
class RemoveOverridden : ArrayList<Int>(), Remove {
    // Overrides Remove.remove but not ArrayList.remove
    override fun remove(element: Int?): Boolean = false
}

class RemoveNotOverridden : ArrayList<Int>(), Remove {
}

class GetBoxedOverridden : MyString(), GetBoxed {
    // Overrides GetBoxed.get but not MyString.charAt
    override fun get(i: Int?): Char? = 'k'
}

class GetPrimitiveOverridden : MyString(), GetPrimitive {
    // Overrides GetPrimitive.get and MyString.charAt
    override fun get(i: Int): Char = 'k'
}

fun box(): String {
    val r1 = RemoveOverridden()
    r1.add(1)
    r1.add(2)

    r1.remove(1 as Int?)
    (r1 as Remove).remove(1)
    if (1 !in r1) return "FAIL 1"

    r1.remove(1)
    if (1 in r1) return "FAIL 2"

    (r1 as ArrayList<Int>).remove(2)
    if (2 in r1) return "FAIL 3"

    val r2 = RemoveNotOverridden()
    r2.add(1)
    r2.add(2)
    r2.add(3)

    (r2 as Remove).remove(1)
    if (1 in r2) return "FAIL 4"

    r2.remove(2)
    if (1 in r2) return "FAIL 5"

    (r2 as ArrayList<Int>).remove(3)
    if (2 in r2) return "FAIL 6"

    val g1 = GetBoxedOverridden()
    if (g1.get(1) != 'j') return "FAIL 7"
    if ((g1 as MyString).get(1) != 'j') return "FAIL 8"
    if ((g1 as GetBoxed).get(1) != 'k') return "FAIL 9"

    val g2 = GetPrimitiveOverridden()
    if (g2.get(1) != 'k') return "FAIL 10"
    if ((g2 as MyString).get(1) != 'k') return "FAIL 11"
    if ((g2 as GetPrimitive).get(1) != 'k') return "FAIL 12"

    return "OK"
}