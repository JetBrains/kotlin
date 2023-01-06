// two.KotlinAbstractClass

// FILE: KotlinAbstractClass.kt
package two

abstract class KotlinAbstractClass : JavaInterface {
    abstract operator fun set(kotlinType: CredentialAttributes, anotherParameter: Boolean)
}

// FILE: two/JavaInterface.java
package two;

import static two.CredentialAttributesKt.CredentialAttributes;

public interface JavaInterface {
    void set(CredentialAttributes attributes);
}

// FILE: CredentialAttributes.kt
package two

class CredentialAttributes

fun CredentialAttributes(i: Int): CredentialAttributes = CredentialAttributes()

const val KOTLIN_CONSTANT = "abcd"
