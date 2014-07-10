fun foo() {
    assert(1 == 1) { "Hahaha" }
}

// assert function will be inlined, and we assure that there are no calls via package part, but is call via package facade in inlined code
// 0 INVOKESTATIC kotlin\/KotlinPackage-[^-]+-[^-]+.getASSERTIONS_ENABLED \(\)Z
// 1 INVOKESTATIC kotlin\/KotlinPackage.getASSERTIONS_ENABLED \(\)Z