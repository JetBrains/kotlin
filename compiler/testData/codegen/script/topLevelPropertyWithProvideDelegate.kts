// JVM_ABI_K1_K2_DIFF: KT-63960, KT-63963

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Test, property: KProperty<*>) = "OK"
}

class Provider {
    operator fun provideDelegate(thisRef: Test, property: KProperty<*>) = Delegate()
}

class Test {
    companion object {
        val instance = Test()
    }

    val message by Provider()
}

val x = Test.instance.message

// expected: x: OK