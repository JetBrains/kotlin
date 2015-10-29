import test.kotlin.A;

import static test.kotlin.JvmOverloadsFunctionsKt.foo;

class JvmOverloadsFunctions {
    public static void main(String[] args) {
        A a = new A() { };

        foo(a.getClass(), a, true, "Some");
        foo(a.getClass(), a, true);
        foo(a.getClass(), a);
    }
}
