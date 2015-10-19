package server

import some.TopLevelMethodFromJava_DataKt

class Testing {
    void test() {
        TopLevelMethodFromJava_DataKt.<caret>foo(12);
    }
}

//INFO: <b>public</b> <b>fun</b> foo(bar: Int): Unit <i>defined in</i> some<p>KDoc foo</p>
