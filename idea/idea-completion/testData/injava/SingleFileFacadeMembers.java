public class Testing {
    void f() {
        mockLib.foo.MainKt.<caret>
    }
}

// EXIST: topLevelFunction
// EXIST: topLevelExtFunction
// EXIST: getTopLevelVar
// EXIST: setTopLevelVar
// LIGHT_CLASS: mockLib.foo.MainKt
