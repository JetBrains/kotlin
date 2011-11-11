package _03_character._12_ABC;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 27). Pearson Education (USA). Kindle Edition.
 */
public class Abc {
    public static void main(String[] args) {
        String letters = "ABC";
        char[] numbers = {'1', '2', '3'};
        System.out.println(letters + " easy as " + numbers);

        // Solution:
        System.out.println(letters + " easy as ");
        System.out.println(numbers);
        // or
        System.out.println(letters + " easy as " + String.valueOf(numbers));
    }
}

