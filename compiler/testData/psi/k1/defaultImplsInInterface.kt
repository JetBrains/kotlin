// JVM_DEFAULT: no-compatibility

package a

interface DefaultImplsInInterface {
    val isVisible: Boolean
    val isPersistent: Boolean
    suspend fun show(mutatePriority: Int)
    fun dismiss()
    fun onDispose()
}
