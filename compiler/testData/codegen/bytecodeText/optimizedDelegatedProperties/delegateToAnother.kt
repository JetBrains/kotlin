// WITH_RUNTIME
class C(var x: Int) {
    val y by C::x
    var ym by C::x
    val z by ::x
    var zm by ::x
}

class D(val c: C) {
    val y by c::x
    var ym by c::x
    val C.z by C::x
    var C.zm by C::x
}

var x = 1
val y by ::x
var ym by ::x
val z by C(1)::x
var zm by C(1)::x

fun local() {
    val y by ::x
    var ym by ::x
    val z by C(1)::x
    var zm by C(1)::x
}

// 0 \$\$delegatedProperties
// 0 kotlin/jvm/internal/PropertyReference[0-2]Impl\.\<init\>

// JVM_IR_TEMPLATES
// Optimized all to direct accesses:
// 0 kotlin/jvm/internal/MutablePropertyReference[0-2]Impl\.\<init\>

// JVM_TEMPLATES
// Not optimized:
// 16 kotlin/jvm/internal/MutablePropertyReference[0-2]Impl\.\<init\>
