// FILE: k.kt

trait G<T>
trait SubG<A, B> : G<String>

trait K<KT> {
    fun foo(): G<KT>
    fun bar(): G<KT>?
}

// FILE: J.java

import java.util.*;

interface J extends K<String> {
    SubG<String, G<String>> foo();
    SubG<String, G<String>> bar();
    SubG<String, G<String>> baz();
}