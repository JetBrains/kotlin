fun foo() {

addMouseListener(object MouseAdapter() {

  private var clickCount = 0;

  override fun mouseClicked(e : MouseEvent) {
    clickCount++;
    if (clickCount > 3) GOD.sendMessage(GodMEssages.TOO_MANY_CLICKS);
  }
})

enum class GodMessages {
  TOO_MANY_CLICKS
  ONE_MORE_MESSAGE
}

// Type of this variable is GOD_AnonymousClass
val GOD = object {
  fun sendMessage(message : GodMEssage) = throw new RuntimeException(message.name)
};


}