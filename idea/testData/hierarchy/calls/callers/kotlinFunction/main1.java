class JA {
    public String name = new KBase().foo("") + new KA().foo("");

    public String foo() {
        return new KBase().foo("") + new KA().foo("");
    }
}