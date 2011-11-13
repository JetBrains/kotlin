package _04_loopy._25_In_the_Loop;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 57). Pearson Education (USA). Kindle Edition.
 */
public class InTheLoop {
    public static final int END = Integer.MAX_VALUE;
    public static final int START = END - 100;

    public static void main(String[] args) {
//        int count = 0;
//        for (int i = START; i <= END; i++)
//            count++;
//        System.out.println(count);
        final int end = Integer.MAX_VALUE;
        int start = end - 100;
        int count = 0;
        for (int i = start; i <= end; i++)
           count++;
        System.out.println("count = " + count);
    }
}

