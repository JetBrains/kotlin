// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

@Retention(AnnotationRetention.RUNTIME)
annotation class Simple(val value: String)

@property:Simple("OK")
val foo: Int = 0

fun box(): String {
    return (::foo.annotations.single() as Simple).value
}
