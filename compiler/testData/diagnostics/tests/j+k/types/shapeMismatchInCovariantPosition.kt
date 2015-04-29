// FILE: k.kt

trait G<T>
trait SubG : G<String>

trait K {
    fun foo(): G<String>
    fun bar(): G<String>?
}

// FILE: J.java

import java.util.*;

interface J extends K {
    SubG foo();
    SubG bar();
    SubG baz();
}