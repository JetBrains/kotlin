class KA {
    public final String name = "A"

    public final String <caret>foo(String s) {
        return "A " + s;
    }
}

class KClient {
    {
        new KA().foo("");
    }

    public static final String a = new KA().foo("");

    public final String getBar() {
        return new KA().foo("");
    }

    public final String bar() {
        return new KA().foo("");
    }
}