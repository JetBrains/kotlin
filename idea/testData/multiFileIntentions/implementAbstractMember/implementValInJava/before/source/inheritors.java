package source;

abstract class X<S> implements T<S> {

}

class Y implements T<String> {

}

class Z implements T<Boolean> {
    @Override
    public Boolean getFoo() {
        return null;
    }
}

interface U extends T<Object> {

}