package _03_character._16_Line_Printer;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 35). Pearson Education (USA). Kindle Edition.
 */
public class LinePrinter {
    public static void main(String[] args) {
        // Note: \ u000A is Unicode representation of linefeed (LF)   <-- DELETE THE SPACE BEFORE 'u'
        char c = 0x000A;
        System.out.println(c);
    }
}

