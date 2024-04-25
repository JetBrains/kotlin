// DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: AImpl.kt

abstract class AImpl {
    fun charAt(index: Int): Char {
        return '1'
    }

    fun length(): Int {
        return 1
    }
}

// FILE: A.java
public class A extends AImpl implements CharSequence {
    public CharSequence subSequence(int start, int end) {
        return null;
    }
}

// FILE: X.kt
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class X<!> : A()

fun main() {
    val x = X()
    x[0]
    x.length
}