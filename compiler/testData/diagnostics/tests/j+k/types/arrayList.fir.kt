// FILE: k.kt

interface ML<T> {
    public fun foo(): MutableList<T>
}

class K : J<String>(), ML<String>

// FILE: J.java

import java.util.*;

public class J<T> extends ML<T> {
    public List<T> foo() { return null; }
}