import testing.Test;

class KotlinClassUsedFromJava {
    void test() {
        <caret>Test();
    }
}

//INFO: <b>internal</b> <b>final</b> <b>class</b> Test <i>defined in</i> testing<br/><p>Some comment
//INFO: </p>
