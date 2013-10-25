class C extends A<String> {
    @Override
    public String getFoo() {
        return super.getFoo();
    }

    @Override
    public void setFoo(String s) {
        super.setFoo(s);
    }
}
