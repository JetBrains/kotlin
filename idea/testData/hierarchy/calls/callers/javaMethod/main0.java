class KBase {
    public String foo(String s) {
        return s;
    }
}

class KA extends KBase {
    public final String name = "A";

    @Override
    public final String <caret>foo(String s) {
        return "A " + s;
    }
}

class KClient {
    {
        new KBase().foo("");
        new KA().foo("");
    }

    public static final String a = new KBase().foo("") + new KA().foo("");

    public final String getBar() {
        return new KBase().foo("") + new KA().foo("");
    }

    public final String bar() {
        return new KBase().foo("") + new KA().foo("");
    }
}