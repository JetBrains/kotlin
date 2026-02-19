// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: some/my/Ann.java
package some.my;

public @interface Ann {}

// MODULE: alias(lib)
// FILE: wrapper/KAnn.kt
package wrapper

import some.my.Ann

public typealias KAnn = Ann

// MODULE: main(alias, lib)
// FILE: main.kt

import wrapper.KAnn

@KAnn
fun box() = "OK"