public class Testing {
    public static void test() {
        mockLib.foo.LibEnum.<caret>
    }
}

// EXIST: RED, GREEN, BLUE
// LIGHT_CLASS: mockLib.foo.LibEnum