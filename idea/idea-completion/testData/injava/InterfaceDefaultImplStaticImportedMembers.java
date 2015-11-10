import static mockLib.foo.MyInterface.DefaultImpls.*;
public class Testing {
    public static void test() {
        f<caret>
    }
}

// EXIST: foo
// LIGHT_CLASS: mockLib.foo.MyInterface