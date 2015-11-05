package test;

public class Testing {
    public static void test() {
        mockLib.foo.LibClass.<caret>
    }
}

// EXIST: Companion
// LIGHT_CLASS: mockLib.foo.LibClass