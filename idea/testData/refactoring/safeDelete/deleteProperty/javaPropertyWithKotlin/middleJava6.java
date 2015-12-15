class C implements B {
    @Override
    public String getFoo() {
        return "C";
    }

    @Override
    public void <caret>setFoo(String value) {

    }
}