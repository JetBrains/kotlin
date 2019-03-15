import testing.KotlinPackageClassUsedFromJava_DataKt;

class KotlinPackageClassUsedFromJava {
    void test() {
        <caret>KotlinPackageClassUsedFromJava_DataKt.foo();
    }
}

//INFO: <div class='definition'><pre>testing<br>public final class <b>testing.KotlinPackageClassUsedFromJava_DataKt</b>
//INFO: extends <a href="psi_element://java.lang.Object"><code>Object</code></a></pre></div><table class='sections'></table>
