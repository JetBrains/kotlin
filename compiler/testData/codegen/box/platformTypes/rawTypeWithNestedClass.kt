// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR

// FILE: OCNewFileActionBase.java
public class OCNewFileActionBase<T extends OCNewFileActionBase<T>.CreateFileDialogBase> {
    public class CreateFileDialogBase { }

    static OCNewFileActionBase get() { return new OCNewFileActionBase(); }
}

// FILE: main.kt
fun box(): String {
    // Before changes in raw types computation: (OCNewFileActionBase<OCNewFileActionBase<*>.CreateFileDialogBase!>..OCNewFileActionBase<out OCNewFileActionBase<*>.CreateFileDialogBase!>?)
    // After that: raw (OCNewFileActionBase<*>..OCNewFileActionBase<*>?)
    val x = OCNewFileActionBase.get()
    return "OK"
}