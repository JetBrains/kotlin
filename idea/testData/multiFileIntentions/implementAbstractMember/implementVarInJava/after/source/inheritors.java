package source;

abstract class X<S> implements T<S> {
    @Override
    public S getFoo() {
        return null;
    }

    @Override
    public void setFoo(S foo) {

    }
}

class Y implements T<String> {
    @Override
    public void setFoo(String s) {

    }

    @Override
    public String getFoo() {
        return null;
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

    @Override
    public Integer getFoo() {
        return null;
    }

    @Override
    public void setFoo(Integer foo) {

    }
}

interface U extends T<Object> {

}