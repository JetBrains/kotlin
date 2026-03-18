// IGNORE_TREE_ACCESS: KT-64899

object Provider {
    operator fun provideDelegate(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Lazy<String> = lazy { property.name }
}

val foo by lazy { "" }
val bar by Provider
