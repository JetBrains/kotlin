package _03_character._20_Whats_My_Class;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 43). Pearson Education (USA). Kindle Edition.
 */
public class Me {
    public static void main(String[] args) {
        System.out.println(Me.class.getName().replaceAll(".", "/") + ".class");
    }
}

