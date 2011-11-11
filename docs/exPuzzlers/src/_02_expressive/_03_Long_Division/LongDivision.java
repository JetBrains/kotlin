package _02_expressive._03_Long_Division;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 9). Pearson Education (USA). Kindle Edition.
 */
public class LongDivision {
    public static void main(String[] args) {
        final long MICROS_PER_DAY = 24 * 60 * 60 * 1000 * 1000;
        final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
        System.out.println(MICROS_PER_DAY / MILLIS_PER_DAY);
    }
}