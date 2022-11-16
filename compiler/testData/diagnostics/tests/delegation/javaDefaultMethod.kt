// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: Interface.java
public interface Interface {
    default void foo() {}
}

// FILE: InterfaceWithoutDefaultMethods.java
public interface InterfaceWithoutDefaultMethods {
    void bar()
}

// FILE: impl.kt
import kotlin.jvm.JvmDelegateToDefaults

open class Impl1: Interface {}

class <!NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD!>Impl2<!> : Interface by Impl1()

class Impl3 : Interface by Impl1() {
    override fun foo() {}
}

class Impl4: Interface {
    override fun foo() {}
}

class <!NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD!>Impl5<!> : Interface by Impl4()

class Impl6: InterfaceWithoutDefaultMethods {
    override fun bar() {}
}

class Impl7: InterfaceWithoutDefaultMethods by Impl6()

public interface Interface2: Interface {
    fun bar()
}

open class Impl8: Interface2 {
    override fun bar() {}
}

class <!NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD!>Impl9<!>: Interface by Impl8()

class Impl10: Interface2 by Impl8()

open class Impl11(): Impl1()
class Impl12(): Impl11()
class <!NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD!>Impl13<!>: Interface by Impl12()

class <!NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD!>Impl14<!>: Interface by (object: Interface {})

class Annotated1 : Interface by @JvmDelegateToDefaults Impl1()

class Annotated2 : Interface by @JvmDelegateToDefaults Impl4()

class Annotated3: Interface by @JvmDelegateToDefaults Impl8()

class Annotated4: Interface by @JvmDelegateToDefaults Impl12()

class Annotated5: Interface by (@JvmDelegateToDefaults object: Interface {})