package source;

abstract class X<S> implements T<S> {

    @Override
    public S foo(S s) {
        return null;
    }
}

class Y implements T<String> {

    @Override
    public String foo(String s) {
        return null;
    }
}

class Z implements T<Boolean> {
    @Override
    public Boolean foo(Boolean b) {
        return null;
    }
}

interface U extends T<Object> {

}