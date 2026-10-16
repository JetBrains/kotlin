// TARGET_BACKEND: JVM
// WITH_STDLIB
// ISSUE: KT-90361
// ISSUE: KT-90363
// IGNORE_BACKEND: JVM
// DISABLE_JAVA_FACADE

// FILE: neg/Marker.kt
package neg

@Retention(AnnotationRetention.RUNTIME)
annotation class Marker(val value: Scope) {
    enum class Scope { A, B }
    class Inner {
        companion object {
            const val CONST: Int = 1
        }
    }
    object Holder {
        val VALUE: Scope = Scope.A
    }
}

@Retention(AnnotationRetention.RUNTIME)
annotation class ArrayMarker(vararg val value: Marker.Scope)

// FILE: neg/Greeter.java
package neg;

import neg.Marker.Scope;

public interface Greeter {
    @ArrayMarker({Scope.A, Scope.MISSING})
    String arrayWithMissingEntry();

    @Marker(Scope.MISSING)
    String missingEntry();

    @Marker(Missing.VALUE)
    String missingQualifier();

    @Marker(com.missing.Scope.VALUE)
    String missingPackage();

    @Marker(Marker.Inner.CONST)
    String nonEnumNestedField();

    @Marker(Marker.Holder.VALUE)
    String objectProperty();
}

// FILE: neg/main.kt
package neg

class GreeterImpl : Greeter {
    override fun arrayWithMissingEntry(): String = "arrayWithMissingEntry"
    override fun missingEntry(): String = "missingEntry"
    override fun missingQualifier(): String = "missingQualifier"
    override fun missingPackage(): String = "missingPackage"
    override fun nonEnumNestedField(): String = "nonEnumNestedField"
    override fun objectProperty(): String = "objectProperty"
}

class Delegator(delegate: Greeter) : Greeter by delegate
