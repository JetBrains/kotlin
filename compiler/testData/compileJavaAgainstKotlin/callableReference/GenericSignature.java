package test;

import kotlin.reflect.KFunction;

class Bar extends Foo {
    @Override
    public KFunction<Request> request() {
        return null;
    }
}
