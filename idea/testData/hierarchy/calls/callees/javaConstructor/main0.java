class KA {
    KA() {
    }

    public final String name = "A";
    public final String foo(String s) {
        return "A " + s;
    }
}

class KClientBase {

}

class KClient extends KClientBase {
    public <caret>KClient() {
        super();
        new KA().foo(new KA().name);
        new JA().foo(new JA().getName());
    }

    {
        new KA().foo(new KA().name);
        new JA().foo(new JA().getName());
    }

    public final void bar() {
        new KA().foo(new KA().name);
        new JA().foo(new JA().getName());
    }
}