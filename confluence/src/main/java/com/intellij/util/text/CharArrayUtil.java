package com.intellij.util.text;

/**
 * @author abreslav
 */
public class CharArrayUtil {
    public static char[] fromSequenceWithoutCopying(CharSequence buffer) {
        char[] result = new char[buffer.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.charAt(i);
        }
        return result;
    }
}
