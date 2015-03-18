import testing.TestingPackage

class KotlinClassUsedFromJava {
    void test() {
        <caret>TestingPackage.foo()
    }
}

//INFO: [light_idea_test_case] testing.TestingPackage...
