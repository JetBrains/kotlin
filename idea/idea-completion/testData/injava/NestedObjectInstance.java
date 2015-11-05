public class Testing {
    public static void test() {
        mockLib.foo.LibClass.Companion.NestedObject.<caret>
    }
}

// EXIST: INSTANCE
// LIGHT_CLASS: mockLib.foo.LibClass