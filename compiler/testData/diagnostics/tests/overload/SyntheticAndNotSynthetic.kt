// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun Runnable(f: () -> Unit): Runnable = object : Runnable {
    public override fun run() {
        f()
    }
}

val x = Runnable {  }