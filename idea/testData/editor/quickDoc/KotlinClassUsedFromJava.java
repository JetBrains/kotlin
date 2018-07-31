import testing.Test;

class KotlinClassUsedFromJava {
    void test() {
        <caret>Test();
    }
}

//INFO: <div class='definition'><pre><a href="psi_element://testing"><code>testing</code></a> <font color="808080"><i>KotlinClassUsedFromJava_Data.kt</i></font><br>public final class <b>Test</b></pre></div><div class='content'><p>Some comment</p></div><table class='sections'></table>
