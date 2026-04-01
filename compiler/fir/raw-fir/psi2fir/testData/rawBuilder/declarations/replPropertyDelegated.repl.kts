// IGNORE_TREE_ACCESS: KT-64899
// IGNORE_BODY_CALCULATOR: KT-85026

object Provider {
    operator fun provideDelegate(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Lazy<String> = lazy { property.name }
}

val foo by lazy { "" }
val bar by Provider
