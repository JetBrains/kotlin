public class Testing {
    public static void test() {
        mockLib.foo.MyInterface.<caret>
    }
}

// EXIST: DefaultImpls
// LIGHT_CLASS: mockLib.foo.MyInterface