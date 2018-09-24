// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

public class RunnableFunctionWrapper(val f : () -> Unit) : Runnable {
    public override fun run() {
        f()
    }
}

fun box() : String {
  var res = ""
  RunnableFunctionWrapper({ res = "OK" }).run()
  return res
}
