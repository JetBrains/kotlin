abstract class I implements T {
    T u;

    @Override
    public int getFoo() {
        return u.getFoo();
    }
}

class J implements T {
    T u;

    @Override
    public int getFoo() {
        return u.getFoo();
    }

    @Override
    public void setFoo(int value) {
        u.setFoo(value);
    }
}