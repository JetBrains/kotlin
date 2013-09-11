class KA {
    public <caret>KA() {

    }

    public final String name = "A";

    public String foo(String s) {
        return "A " + s;
    }
}

class KClient {
    {
        new KA();
    }

    public static final a = new KA();

    public final String getBar() {
        return new KA().name;
    }

    public final KA bar() {
        return new KA();
    }
}