// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: kt46092.kt

class CharSequenceBackedByChars : CharArrayCharSequence {
    constructor(chars: CharArray) : super(*chars)

    fun test(): String = string
}

fun box() = CharSequenceBackedByChars(charArrayOf('O', 'K')).test()

// FILE: CharArrayCharSequence.java

public class CharArrayCharSequence {
    protected final String string;

    public CharArrayCharSequence(char... chars) {
        string = new String(chars);
    }
}
