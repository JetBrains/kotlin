package _02_expressive._07_Swap_Meat;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 17). Pearson Education (USA). Kindle Edition.
 */
public class CleverSwap {
    public static void main(String[] args) {
        int x = 1984; // (0x7c0)
        int y = 2001; // (0x7d1)
        x ^= y ^= x ^= y;
        System.out.println("x = " + x + "; y = " + y);

        // Solution: swap with a temp variable
        // Do not assign to the same variable more than once in a single expression.
    }
}