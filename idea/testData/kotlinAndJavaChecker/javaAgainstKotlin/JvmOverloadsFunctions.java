import test.kotlin.A;

import static test.kotlin.JvmOverloadsFunctionsKt.foo;

class JvmOverloadsFunctions {
    public static void main(String[] args) {
        A a = new A() { };

        foo(a.getClass(), a, true, "Some");
        foo(a.getClass(), a, true);
        foo(a.getClass(), a);

        // Before KT-28556 is fixed the second not-nullable parameter wasn't marked as it shoud, so there were no warnings on it
        foo(<warning descr="Passing 'null' argument to parameter annotated as @NotNull">null</warning>, <warning descr="Passing 'null' argument to parameter annotated as @NotNull">null</warning>, true, <warning descr="Passing 'null' argument to parameter annotated as @NotNull">null</warning>);
        foo(<warning descr="Passing 'null' argument to parameter annotated as @NotNull">null</warning>, <warning descr="Passing 'null' argument to parameter annotated as @NotNull">null</warning>, true);
        foo(<warning descr="Passing 'null' argument to parameter annotated as @NotNull">null</warning>, <warning descr="Passing 'null' argument to parameter annotated as @NotNull">null</warning>);
    }
}
