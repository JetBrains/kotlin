fun foo() {
    assert(1 == 1) { "Hahaha" }
}

// 0 INVOKESTATIC kotlin\/KotlinPackage\.getASSERTIONS_ENABLED
// 1 INVOKESTATIC kotlin\/PreconditionsKt\.getASSERTIONS_ENABLED
// 0 INVOKESTATIC kotlin\/PreconditionsKt__.+\.getASSERTIONS_ENABLED
