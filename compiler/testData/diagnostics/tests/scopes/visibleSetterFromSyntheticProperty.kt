// FIR_IDENTICAL

// FILE: Java.java

public abstract class Java {
    private String _name = null;

    void setName(String name) {
        this._name = name;
    }

    public String getName() {
        return _name;
    }
}

// FILE: main.kt

fun Java.test(name: String) {
    this.name = name
}