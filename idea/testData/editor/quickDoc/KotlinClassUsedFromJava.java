import testing.Test;

class KotlinClassUsedFromJava {
    void test() {
        <caret>Test();
    }
}

//INFO: <pre><b>public</b> <b>final</b> <b>class</b> Test <i>defined in</i> testing <i>in file</i> KotlinClassUsedFromJava_Data.kt</pre><p>Some comment</p>
