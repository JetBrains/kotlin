// WITH_STDLIB

val topLevelLazyVal by lazy { 1 }

class C {
    val memberLazyVal by lazy { 2 }
}

fun box(): String {
    val localLazyVal by lazy { 3 }

    if (topLevelLazyVal != 1) throw AssertionError()
    if (C().memberLazyVal != 2) throw AssertionError()
    if (localLazyVal != 3) throw AssertionError()

    return "OK"
}

// 0 \$\$delegatedProperties
// 0 kotlin/jvm/internal/PropertyReference0Impl\.\<init\>
// 0 kotlin/jvm/internal/MutablePropertyReference0Impl\.\<init\>