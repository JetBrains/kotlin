package server

import some.SomePackage

class Testing {
    void test() {
        SomePackage.<caret>foo(12);
    }
}

//INFO: <b>internal</b> <b>fun</b> foo(bar: Int): Unit <i>defined in</i> some<p>KDoc foo</p>
