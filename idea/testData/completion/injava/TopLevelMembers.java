public class Testing {
    public static void test() {
        mockLib.foo.FooPackage.<caret>
    }
}

// EXIST: topLevelFunction
// EXIST: topLevelExtFunction
// EXIST: getTopLevelVar, setTopLevelVar
// EXIST: anotherTopLevelFunction
