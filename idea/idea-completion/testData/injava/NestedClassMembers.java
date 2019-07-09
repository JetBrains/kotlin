package test;

public class Testing {
    public static void test(mockLib.foo.LibClass.Nested p) {
        p.<caret>
    }
}

// EXIST: getValInNested
// EXIST: funInNested