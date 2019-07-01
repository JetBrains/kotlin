public class Testing {
    void f() {
        mockLib.foo.MainKt.<caret>
    }
}

// EXIST: topLevelFunction
// EXIST: topLevelExtFunction
// EXIST: getTopLevelVar
// EXIST: setTopLevelVar
