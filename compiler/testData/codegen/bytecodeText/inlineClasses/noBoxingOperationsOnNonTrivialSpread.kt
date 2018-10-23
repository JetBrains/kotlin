// WITH_UNSIGNED

fun uint(vararg us: UInt): UIntArray = us

// FILE: NoBoxing.kt

fun test1(us: UIntArray) {
    uint(1u, *us, 2u, *us)
}

// @NoBoxingKt.class:
// 0 INVOKESTATIC kotlin.UInt\$Erased.box
// 0 INVOKESTATIC kotlin.UInt\.box
// 0 INVOKEVIRTUAL kotlin.UInt.unbox

// FILE: Boxing.kt

fun nullableUInt(vararg us: UInt?) {}

fun test2(nullable: UInt?, ns: Array<UInt>) {
    nullableUInt(1u, nullable, 3u, *ns)
}

// @BoxingKt.class:
// 2 INVOKESTATIC kotlin.UInt\.box
// 0 INVOKEVIRTUAL kotlin.UInt.unbox