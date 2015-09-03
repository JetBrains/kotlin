public class ImplementJavaInnerInterface {
    interface Test {
        void foo();
    }

    void test() {
        Test test = new Test() {
            @Override
            public void foo() {

            }
        };
    }

    void usage(Test test) {
        test.<caret>foo();
    }
}

// REF: (in KotlinTest).foo()
// REF: <anonymous>.foo()