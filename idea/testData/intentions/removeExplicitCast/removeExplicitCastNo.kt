// IS_APPLICABLE: false
public class Foo {
    fun foose() {}
}

public class Testing {

    fun demo(x: Any) {

        <caret>x.foose() as Foo
    }
}