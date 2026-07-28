// FILE: DefaultArgumentInEnumConstructor.java
// KT-7892 Parameter with default value in enum's constructor breaks Java compilation

package test;

public class DefaultArgumentInEnumConstructor {
    static K entry = K.ENTRY;
}

// FILE: DefaultArgumentInEnumConstructor.kt
package test

enum class K(private val default: String = "default") {
    ENTRY()
}
