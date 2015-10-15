import testing.KotlinPackageClassUsedFromJava_DataKt;

class KotlinPackageClassUsedFromJava {
    void test() {
        <caret>KotlinPackageClassUsedFromJava_DataKt.foo();
    }
}

//INFO: [light_idea_test_case] testing...
