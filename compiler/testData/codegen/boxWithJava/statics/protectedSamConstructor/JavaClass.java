public class JavaClass {

    public String runZ(Z z) {
        return z.run("O", "K");
    }

    protected interface Z {
        String run(String s1, String s2);
    }
}