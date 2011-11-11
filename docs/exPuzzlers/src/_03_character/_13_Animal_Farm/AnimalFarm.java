package _03_character._13_Animal_Farm;

/**
 * Bloch, Joshua; Gafter, Neal (2005-06-24). Java™ Puzzlers: Traps, Pitfalls, and Corner Cases (p. 29). Pearson Education (USA). Kindle Edition.
 */
public class AnimalFarm {
    public static void main(String[] args) {
        final String pig = "length: 10";
        final String dog = "length: " + pig.length();
        System.out.println("Animals are equal: " + pig == dog);

        // Solution:
        System.out.println("Animals are equal: " + pig.equals(dog));
        // or
        System.out.println("Animals are equal: " + (pig.intern() == dog.intern()));
    }
}

