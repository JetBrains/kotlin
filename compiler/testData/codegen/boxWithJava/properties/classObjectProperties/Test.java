class Test {
    String test() {
        String s;

        s = Klass.NAME;
        if (!s.equals("Klass")) throw new AssertionError("Fail class: " + s);

        s = Trait.NAME;
        if (!s.equals("Trait")) throw new AssertionError("Fail interface: " + s);

        s = Trait.DEPRECATED;
        if (!s.equals("DEPRECATED")) throw new AssertionError("Fail interface: " + s);

        s = Enoom.NAME;
        if (!s.equals("Enum")) throw new AssertionError("Fail enum: " + s);

        return "OK";
    }
}
