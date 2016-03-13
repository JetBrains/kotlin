// FILE: JavaClass2.java

class JavaClass1 {
    protected Object value = null;

    public Object getSomething() { return null; }
    public void setSomething(Object value) {  this.value = value; }
}

class JavaClass2 extends JavaClass1 {
    public String getSomething() { return (String)value; }
}

// FILE: 1.kt

fun box(): String {
    val javaClass = JavaClass2()
    javaClass.something = "OK"
    return javaClass.something
}
