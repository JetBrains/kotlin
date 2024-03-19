// ISSUE: KT-58575
// FILE: JavaBases.java
interface JavaImmutableBase {
    String getData();
}
interface JavaMutableBase {
    String getData();
    void setData(String data);
}

// FILE: KotlinChildren.kt
open class KotlinImmutableChild1: JavaImmutableBase {
    private val data: String = ""
    override fun getData(): String = ""
}
open class KotlinImmutableChild2: JavaImmutableBase {
    private val data: String? = ""
    override fun getData(): String = ""
}
open class KotlinImmutableChild4: JavaImmutableBase {
    private val data: String? = ""
    override fun getData(): String? = ""
}
open class KotlinMutableChild1: JavaMutableBase {
    private var data: String = ""
    override fun getData(): String = ""
    override fun setData(data: String) {}
}
open class KotlinMutableChild6: JavaMutableBase {
    private var data: String? = ""
    override fun getData(): String = ""
    override fun setData(data: String?) {}
}
open class KotlinMutableChild8: JavaMutableBase {
    private var data: String? = ""
    override fun getData(): String? = ""
    override fun setData(data: String?) {}
}

// FILE: JavaChildren.java
class JavaImmutableChild1 extends KotlinImmutableChild1 {}
class JavaImmutableChild2 extends KotlinImmutableChild2 {}
class JavaImmutableChild4 extends KotlinImmutableChild4 {}
class JavaMutableChild1 extends KotlinMutableChild1 {}
class JavaMutableChild6 extends KotlinMutableChild6 {}
class JavaMutableChild8 extends KotlinMutableChild8 {}

// FILE: Main.kt
fun unresolvedReferences() {
    JavaImmutableChild1().getData()
    JavaImmutableChild2().getData()
    JavaImmutableChild4().getData()
    JavaMutableChild1().getData()
    JavaMutableChild1().setData("")
    JavaMutableChild6().getData()
    JavaMutableChild6().setData("")
    JavaMutableChild8().getData()
    JavaMutableChild8().setData("")
}
