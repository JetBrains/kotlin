package _02_expressive._02_Time_for_a_Change;

import java.math.BigDecimal;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 7). Pearson Education (USA). Kindle Edition.
 */
public class ChangeSolutions {
    public static void main(String args[ ] ) {
        // Poor solution - still uses binary floating-point!
        System.out.printf("%.2f%n", 2.00 - 1.10);

        // Calculation in cents
        System.out.println((200 - 110) + " cents");

        // BigDecimal
        System.out.println(new BigDecimal("2.00"). subtract(new BigDecimal("1.10")));
    }
}

