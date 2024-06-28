// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: BaseKotlin.kt
open class BaseKotlin : BaseJava() {
    override fun setSomething(s: String) {

    }
}

// FILE: BaseJava.java
public class BaseJava {
    public String getSomething() {
        return "";
    }

    public void setSomething(String s) {

    }
}

// MODULE: main(lib)
// MEMBER_NAME_FILTER: something
// FILE: main.kt
class Deri<caret>ved : BaseKotlin() {
    override fun getSomething(): String = "42"
}
