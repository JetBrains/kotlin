// KJS_WITH_FULL_RUNTIME
//WITH_REFLECT

import kotlin.properties.Delegates

fun box(): String {
    var foo: String by Delegates.notNull();

    object {
        fun baz() {
            foo = "OK"
        }
    }.baz()
    return foo
}
