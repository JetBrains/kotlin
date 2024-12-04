// ISSUE: KT-65004

package setterVisability

abstract class ChatViewModel {
    protected abstract val chatId: Long?
}

class ChatGroupViewModel: ChatViewModel() {
    override var chatId: Long = TODO()
}
