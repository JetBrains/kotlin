// ISSUE: KT-44804
// WITH_STDLIB

abstract class AbstractInsnNode(val next: AbstractInsnNode? = null)

class LineNumberNode(next: AbstractInsnNode? = null) : AbstractInsnNode(next) {
    val line: Int = 1
}

class LabelNode() : AbstractInsnNode(null)

fun isDeadLineNumber(insn: LineNumberNode, index: Int, frames: Array<out Any?>): Boolean {
    // Line number node is "dead" if the corresponding line number interval
    // contains at least one "dead" meaningful instruction and no "live" meaningful instructions.
    var finger: AbstractInsnNode = insn
    var fingerIndex = index
    var hasDeadInsn = false
    loop@ while (true) {
        finger = finger.next ?: break
        fingerIndex++
        when (finger) {
            is LabelNode ->
                continue@loop
            is LineNumberNode ->
                if (finger.line != insn.line) return hasDeadInsn
            else -> {
                if (frames[fingerIndex] != null) return false
                hasDeadInsn = true
            }
        }
    }
    return true
}

fun box(): String {
    val node = LineNumberNode(
        LineNumberNode(
            LabelNode()
        )
    )
    val result = isDeadLineNumber(node, 0, arrayOf(null, null, "aaa", "bbb"))
    return if (result) "OK" else "fail"
}
