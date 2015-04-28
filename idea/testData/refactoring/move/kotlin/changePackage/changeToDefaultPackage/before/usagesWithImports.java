import source.Foo;
import static source.SourcePackage.foo;

class Test {
    static void test() {
        new Foo();
        foo();
    }
}