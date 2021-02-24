fun test() {
    fun local(s: Int) {
    }

    local(1)
}

// JVM_TEMPLATES
// 2 invoke \(I\)V

// JVM_IR_TEMPLATES
// 1 INVOKESTATIC LocalFunKt\.test\$local \(I\)V