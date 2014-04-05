public class Foo {
    fun foose() {}
}

public class Testing {

    fun demo(x: Any) {

        if (x is Foo) {
            <caret>x.foose() as Foo
        }
    }
}