// WITH_STDLIB
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

// Optimized all to direct accesses, with `$delegate` methods generating reflected references on demand:
// 0 extends kotlin/jvm/internal/MutablePropertyReference[0-2]Impl
// 0 private final( static)? Lkotlin/reflect/KMutableProperty[0-2]; [xyz]m?\$delegate
// 2 private final( static)? LC; [xyz]m?\$receiver
// 0 LOCALVARIABLE [xyz]m? Lkotlin/reflect/KMutableProperty[0-2];
// 12 private( static)? get[XYZ]m?\$delegate
