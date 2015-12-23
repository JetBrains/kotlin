// "Change parameter 'a' type of function 'test.B.foo' to 'String'" "true"
package test;

import org.jetbrains.annotations.NotNull;

class J extends B {
    @Override
    void foo(@NotNull String a) {
        super.foo(a);
    }
}