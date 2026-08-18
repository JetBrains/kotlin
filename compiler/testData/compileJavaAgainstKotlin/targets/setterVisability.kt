// ISSUE: KT-65004

// FILE: setterVisability.java
import setterVisability.ChatGroupViewModel;

class Test {
    public void test_1(ChatGroupViewModel setterVisability) {
        setterVisability.chatId = 1;
    }
}

// FILE: setterVisability.kt
package setterVisability

abstract class ChatViewModel {
    protected abstract val chatId: Long?
}

class ChatGroupViewModel: ChatViewModel() {
    override var chatId: Long = TODO()
}
