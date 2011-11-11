package _02_expressive._01_Oddity;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 5). Pearson Education (USA). Kindle Edition.
 */
public class Oddity {
    public static boolean isOdd(int i) {
        return i % 2 == 1;
    }

    public static void main(String[] args) {
        System.out.println(isOdd(-1));
    }
}
