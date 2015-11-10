import mockLib.foo

public class Testing {
    public static void test() {
        new LibClass.<caret>
    }
}

// EXIST: LibClass.Nested
// LIGHT_CLASS: mockLib.foo.LibClass