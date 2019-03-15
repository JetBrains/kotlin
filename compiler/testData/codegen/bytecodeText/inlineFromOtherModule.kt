// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=legacy
fun foo() {
    assert(1 == 1) { "Hahaha" }
}

// 1 GETSTATIC kotlin\/_Assertions\.ENABLED
