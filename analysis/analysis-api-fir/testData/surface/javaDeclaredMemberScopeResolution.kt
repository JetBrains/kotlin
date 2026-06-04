// FILE: main.kt
open class Base {
    val baseProperty: String = "base"
    fun baseFunction(): String = privateBaseFunction()

    private fun privateBaseFunction(): String = "foo"
}

// FILE: Impl.java
public class Impl extends Base {
    public int implField = 1;

    public int implMethod() {
        return implField;
    }

    private String privateImplMethod() {
        return "hoge";
    }
}
