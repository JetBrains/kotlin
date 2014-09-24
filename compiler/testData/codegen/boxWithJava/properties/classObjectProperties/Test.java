class Test {
    String test() {
        String s;

        s = Klass.NAME;
        if (!s.equals("Klass")) throw new AssertionError("Fail class: " + s);

        s = Trait.NAME;
        if (!s.equals("Trait")) throw new AssertionError("Fail trait: " + s);

        s = Enoom.NAME;
        if (!s.equals("Enoom")) throw new AssertionError("Fail enum: " + s);

        return "OK";
    }
}
