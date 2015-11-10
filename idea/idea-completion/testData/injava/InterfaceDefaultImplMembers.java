public class Testing {
    public static void test() {
        mockLib.foo.MyInterface.DefaultImpls.<caret>
    }
}

// EXIST: foo
// LIGHT_CLASS: mockLib.foo.MyInterface