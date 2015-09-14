package testing;

import testing.rename.BarKt;

class JavaClient {
    public void foo() {
        String old = BarKt.getFoo();
        BarKt.setFoo(old + "new");
    }
}