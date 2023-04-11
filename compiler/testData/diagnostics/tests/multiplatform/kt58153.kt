// FIR_IDENTICAL
// ISSUE: KT-58153

// MODULE: m1-common
// FILE: common.kt

expect open class LockFreeLinkedListNode()

class NodeList: LockFreeLinkedListNode() {
    override fun toString(): String = ""
}

// MODULE: m2-jvm()()(m1-common)
// FILE: platform.kt

actual open class LockFreeLinkedListNode {
    override fun toString(): String = ""
}