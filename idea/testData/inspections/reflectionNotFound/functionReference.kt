package test

// WITH_RUNTIME

import kotlin.reflect.KFunction0

fun foo() {}

fun bar(f: () -> Unit) = f()

// Inspection should be reported here because '::foo' may be used as a reflection object
val p1 = ::foo                    // the type is KFunction0 by default
val p2: KFunction0<Unit> = ::foo  // the expected type is KFunction0

// But shouldn't be reported here
val p3 = bar(::foo)               // the expected type is Function0, '::foo' is used as an ordinary function
val p4: Any = ::foo               // the expected type is Any
val p5: UnresolvedClass = ::foo   // an error, another warning would be useless
