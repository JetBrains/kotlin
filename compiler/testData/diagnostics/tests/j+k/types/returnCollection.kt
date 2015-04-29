// FILE: k.kt

trait K {
    fun foo(): List<String>
}

// FILE: J.java

import java.util.*;

interface J extends K {
    List<String> foo();
}