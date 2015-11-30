package test;

import kotlin.jvm.functions.*;

public class MethodWithFunctionTypes {
    public Function0<String> foo(Function1<String, String> f) {
        throw new UnsupportedOperationException();
    }
}
