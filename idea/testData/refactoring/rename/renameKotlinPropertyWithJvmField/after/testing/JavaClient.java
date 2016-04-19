package testing;

import testing.rename.C;

class JavaClient {
    public void foo(C c) {
        int n = c.second;
        c.second = 2;
    }
}
