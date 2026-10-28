// FILE: jlib/ConstantDesc.java
package jlib;

public sealed interface ConstantDesc permits ClassDesc, MethodHandleDesc, DynamicConstantDesc, StringDesc {}

// FILE: jlib/ClassDesc.java
package jlib;

public sealed interface ClassDesc extends ConstantDesc permits PrimitiveClassDescImpl, ReferenceClassDescImpl {}

// FILE: jlib/PrimitiveClassDescImpl.java
package jlib;

final class PrimitiveClassDescImpl implements ClassDesc {}

// FILE: jlib/ReferenceClassDescImpl.java
package jlib;

final class ReferenceClassDescImpl implements ClassDesc {}

// FILE: jlib/MethodHandleDesc.java
package jlib;

public sealed interface MethodHandleDesc extends ConstantDesc permits AsTypeMethodHandleDesc, DirectMethodHandleDesc {}

// FILE: jlib/AsTypeMethodHandleDesc.java
package jlib;

final class AsTypeMethodHandleDesc extends DynamicConstantDesc implements MethodHandleDesc {}

// FILE: jlib/DirectMethodHandleDesc.java
package jlib;

public sealed interface DirectMethodHandleDesc extends MethodHandleDesc permits DirectMethodHandleDescImpl {}

// FILE: jlib/DirectMethodHandleDescImpl.java
package jlib;

final class DirectMethodHandleDescImpl implements DirectMethodHandleDesc {}

// FILE: jlib/DynamicConstantDesc.java
package jlib;

public abstract non-sealed class DynamicConstantDesc implements ConstantDesc {}

// FILE: jlib/StringDesc.java
package jlib;

public final class StringDesc implements ConstantDesc {}

// FILE: main.kt
import jlib.ConstantDesc

fun test(d: ConstantDesc) {
    <caret>when (d) {
    }
}
