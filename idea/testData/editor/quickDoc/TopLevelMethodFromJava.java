package server

import some.TopLevelMethodFromJava_DataKt

class Testing {
    void test() {
        TopLevelMethodFromJava_DataKt.<caret>foo(12);
    }
}

//INFO: <div class='definition'><pre><a href="psi_element://some"><code>some</code></a> (TopLevelMethodFromJava_Data.kt)<br><b>public</b> <b>fun</b> foo(
//INFO:     bar: Int
//INFO: ): Unit</pre></div><div class='content'><p>KDoc foo</p></div><table class='sections'></table>
