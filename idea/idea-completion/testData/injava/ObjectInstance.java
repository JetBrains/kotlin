public class Testing {
    public static void test() {
        mockLib.foo.LibObject.<caret>
    }
}

// EXIST: INSTANCE
// LIGHT_CLASS: mockLib.foo.LibObject