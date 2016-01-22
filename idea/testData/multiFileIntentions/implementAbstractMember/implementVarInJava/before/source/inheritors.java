package source;

abstract class X<S> implements T<S> {
    @Override
    public S getFoo() {
        return null;
    }
}

class Y implements T<String> {
    @Override
    public void setFoo(String s) {

    }
}

class Z implements T<Boolean> {
    @Override
    public Boolean getFoo() {
        return null;
    }

    @Override
    public void setFoo(Boolean b) {

    }
}

class W implements T<Integer> {

}

interface U extends T<Object> {

}