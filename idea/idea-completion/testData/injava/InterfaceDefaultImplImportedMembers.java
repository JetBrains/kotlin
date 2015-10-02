import mockLib.foo.MyInterface.DefaultImpls;
public class Testing {
    public static void test() {
        DefaultImpls.<caret>
    }
}

// EXIST: foo
