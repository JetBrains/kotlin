import testing.Test;

class KotlinClassUsedFromJava {
    void test() {
        <caret>Test();
    }
}

//INFO: <pre><b>public</b> <b>final</b> <b>class</b> Test <i>defined in</i> testing</pre><p>Some comment</p>
