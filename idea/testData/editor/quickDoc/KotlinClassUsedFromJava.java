import testing.Test;

class KotlinClassUsedFromJava {
    void test() {
        <caret>Test();
    }
}

//INFO: <div class='definition'><pre><a href="psi_element://testing"><code>testing</code></a> (KotlinClassUsedFromJava_Data.kt)<br><b>public</b> <b>final</b> <b>class</b> Test</pre></div><div class='content'><p>Some comment</p></div><table class='sections'></table>
