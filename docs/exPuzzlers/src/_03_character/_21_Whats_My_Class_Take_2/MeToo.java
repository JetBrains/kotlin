package _03_character._21_Whats_My_Class_Take_2;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 45). Pearson Education (USA). Kindle Edition.
 */

public class MeToo {
    public static void main(String[] args) {
        System.out.println(MeToo.class.getName().
                replaceAll("\\.", /*File.separator*/"\\") + ".class");
    }
}

