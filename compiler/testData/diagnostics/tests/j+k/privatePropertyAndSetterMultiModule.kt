// FIR_IDENTICAL
// SKIP_TXT

// MODULE: lib

// FILE: JBaseInterface.java
public interface JBaseInterface {
    String getData2();
    void setData2(String data);

    String getData4();
    void setData4(String data);

    String getData5();
    void setData5(String data);
}

// FILE: KIntermediateClass.kt
open class KIntermediateClass : JBaseInterface {
    private var data2: String? = ""
    override fun getData2(): String = ""
    override fun setData2(data: String) {}

    private var data4: String? = ""
    override fun getData4(): String? = ""
    override fun setData4(data: String) {}

    private var data5: String = ""
    override fun getData5(): String = ""
    override fun setData5(data: String?) {}
}

// FILE: JChildClass.java
public class JChildClass extends KIntermediateClass {}

// MODULE: main(lib)

// FILE: Main.kt
fun main(editorTabs: JChildClass) {
    editorTabs.getData2()
    editorTabs.setData2("")
    editorTabs.getData4()
    editorTabs.setData4("")
    editorTabs.getData5()
    editorTabs.setData5("")
}
