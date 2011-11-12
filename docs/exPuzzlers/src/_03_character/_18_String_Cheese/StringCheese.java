package _03_character._18_String_Cheese;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 39). Pearson Education (USA). Kindle Edition.
 */
public class StringCheese {
    public static void main(String[] args) {
        byte[] bytes = new byte[256];
        for (int i = 0; i < 256; i++)
            bytes[i] = (byte) i;
        String str = new String(bytes);
        for (int i = 0, n = str.length(); i < n; i++)
            System.out.print((int) str.charAt(i) + " ");
    }
}

