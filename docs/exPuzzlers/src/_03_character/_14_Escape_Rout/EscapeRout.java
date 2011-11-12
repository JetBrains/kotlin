package _03_character._14_Escape_Rout;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 31). Pearson Education (USA). Kindle Edition.
 */
public class EscapeRout {
    public static void main(String[ ] args) {
        // \u0022 is the Unicode escape for double quote (")
        System.out.println("a\u0022.length( ) + \u0022b".length( ));
    }
}

