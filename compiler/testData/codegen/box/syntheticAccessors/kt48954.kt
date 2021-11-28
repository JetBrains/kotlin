// TARGET_BACKEND: JVM
// FULL_JDK
// JVM_TARGET: 1.8

// FILE: kt48954.kt
import lib.*
import app.*

fun box(): String {
    val foo = Foo()
    Service(foo::bar).test()
    return foo.p
}

// FILE: app/Service.kt
package app

import java.util.function.Consumer

class Service(private val consumer: Consumer<String>) {
    fun test() {
        consumer.accept("OK")
    }
}

// FILE: lib/PackagePrivateBase.java
package lib;

class PackagePrivateBase {
    public String p;

    public void bar(String param) {
        this.p = param;
    }
}

// FILE: lib/Foo.java
package lib;

public class Foo extends PackagePrivateBase {
}
