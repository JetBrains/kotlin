package _02_expressive._05_The_Joy_of_Hex;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 13). Pearson Education (USA). Kindle Edition.
 */
public class JoyOfHex {
    public static void main(String[] args) {
        System.out.println(Long.toHexString(0x100000000L + 0xcafebabe));
    }
}

