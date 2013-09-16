public class Testing {
    public static void test() {
        jettesting.some.<caret>
    }
}

// EXIST: ClassFromJet
// EXIST: SomePackage
// INVOCATION_COUNT: 2
