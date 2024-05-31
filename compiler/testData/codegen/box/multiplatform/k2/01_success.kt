// LANGUAGE: +MultiPlatformProjects
// ALLOW_KOTLIN_PACKAGE
// TARGET_BACKEND: JVM_IR

// MODULE: common
// FILE: common.kt
package foo

public expect open class AbstractMutableList()

public class MutableListImpl: AbstractMutableList()

// MODULE: jvm()()(common)
// FILE: bar/JavaAbstractMutableList.java
package bar; // Java class is in the different package.

public class JavaAbstractMutableList {}

// FILE: jvm.kt
package foo

public actual open class AbstractMutableList: bar.JavaAbstractMutableList()

// FILE: box.kt

import foo.*

fun box(): String {
    val l = MutableListImpl()
    return "OK"
}
