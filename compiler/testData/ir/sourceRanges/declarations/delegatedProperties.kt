// WITH_STDLIB
// ISSUE: KT-59864

import kotlin.properties.Delegates

class MyClass {
    val lazyProp by lazy {
        5
    }

    var observableProp: String by Delegates.observable("<none>") { prop, old, new ->
        println("Was $old, now $new")
    }
}
