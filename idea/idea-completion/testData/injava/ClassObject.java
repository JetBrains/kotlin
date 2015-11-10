public class Testing {
    public static void test() {
        mockLib.foo.LibClass.Companion.<caret>
    }
}

// EXIST: classObjectFun
// LIGHT_CLASS: mockLib.foo.LibClass