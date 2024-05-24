// FILE: main.kt
/**
 * [Storage.<caret_1>value]
 * [Storage.<caret_2>setValue]
 * [Storage.<caret_3>prop]
 */
fun usage() {

}

// FILE: Storage.java
class Storage {
    void prop() {}
    void setValue(String value) {}
    String getProp() { return null; }
}
