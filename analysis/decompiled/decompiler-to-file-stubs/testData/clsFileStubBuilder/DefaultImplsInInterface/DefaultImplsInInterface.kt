// JVM_DEFAULT: all
// BINARY_STUB_ONLY_TEST

package a

interface DefaultImplsInInterface {
    val isVisible: Boolean
    val isPersistent: Boolean
    suspend fun show(mutatePriority: Int)
    fun dismiss()
    fun onDispose()
}