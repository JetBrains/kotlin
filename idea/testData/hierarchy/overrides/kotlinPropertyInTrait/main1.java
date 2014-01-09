interface A extends T {
    @Override
    public String getFoo();

    @Override
    public void setFoo(String value);
}

class B implements T {
    @Override
    public String getFoo() {

    }

    @Override
    public void setFoo(String value) {

    }
}

class C implements A {
    @Override
    public String getFoo() {

    }

    @Override
    public void setFoo(String value) {

    }
}

class D extends Z {
    @Override
    public String getFoo() {

    }

    @Override
    public void setFoo(String value) {

    }
}

class S {

}