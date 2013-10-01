class KA {
    public KA() {

    }

    public final String name = "A";

    public final String foo(String s) {
        return "A " + s;
    }
}

class KClient {
    public final void <caret>bar() {
        new KA().foo("");
        new JA().foo(new JA().getName());

        new Runnable() {
            public void run() {
                new KA().foo("");
                new JA().foo(new JA().getName());
            }
        }.run();
    }
}