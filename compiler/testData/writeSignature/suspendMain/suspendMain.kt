// WITH_RUNTIME
suspend fun f(): Int {
  return 4
}

suspend fun main(args: Array<String>) {
  f()
}

// method: SuspendMainKt::main
// jvm signature:     ([Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;
// generic signature: ([Ljava/lang/String;Lkotlin/coroutines/Continuation<-Lkotlin/Unit;>;)Ljava/lang/Object;

// method: SuspendMainKt::main
// jvm signature:     ([Ljava/lang/String;)V
// generic signature: null

