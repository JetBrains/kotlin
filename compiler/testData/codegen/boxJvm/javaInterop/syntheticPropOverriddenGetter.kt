// TARGET_BACKEND: JVM
// FILE: Component.java

public abstract class Component {
    public void setPreferredSize(Object preferredSize) {}
    public Object getPreferredSize() { return new Object(); }
}

// FILE: ProjectMain.kt

class ComboBox<T>: Component() {
    override fun getPreferredSize(): Any? = "OK"
}

fun box(): String {
    val comboBox = ComboBox<Int>()
    comboBox.preferredSize = Any()
    return comboBox.preferredSize as String
}
