package test;

public class Testing {
    public static void test() {
        mockLib.foo.<caret>
    }
}

// note: "NUMBER" check also assures that FooPackage$src$ classes are not visible

// EXIST: LibClass
// EXIST: LibTrait
// EXIST: LibEnum
// EXIST: LibObject
// EXIST: MainKt
// EXIST: AnotherKt