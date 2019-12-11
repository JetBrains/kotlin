// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

operator fun provideDelegate(x: Any?, p: KProperty<*>) {}

operator fun Any.provideDelegate(x: Any?, p: KProperty<*>) {}

operator fun Any.provideDelegate(x: Any?, p: Any) {}

operator fun Any.provideDelegate(x: Any?, p: Int) {}

class Host1 {
    operator fun provideDelegate(x: Any?, p: KProperty<*>) {}
}

class Host2 {
    operator fun Any.provideDelegate(x: Any?, p: KProperty<*>) {}
}

class Host3 {
    operator fun provideDelegate(x: Any?, p: KProperty<*>, foo: Int) {}
}

class Host4 {
    operator fun provideDelegate(x: Any?, p: KProperty<*>, foo: Int = 0) {}
}

class Host5 {
    operator fun provideDelegate(x: Any?, p: KProperty<*>, vararg foo: Int) {}
}

