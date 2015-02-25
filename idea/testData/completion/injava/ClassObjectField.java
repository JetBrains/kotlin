public class Testing {
    public static void test() {
        mockLib.foo.LibClass.<caret>
    }
}

// EXIST: Default
// EXIST: OBJECT$