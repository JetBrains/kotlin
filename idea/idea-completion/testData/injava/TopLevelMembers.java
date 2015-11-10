public class Testing {
    public static void test() {
        mockLib.foo.MainKt.<caret>
    }
}

// EXIST: topLevelFunction
// EXIST: topLevelExtFunction
// EXIST: getTopLevelVar, setTopLevelVar
// LIGHT_CLASS: mockLib.foo.MainKt