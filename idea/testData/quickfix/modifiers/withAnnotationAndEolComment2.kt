// "Remove 'final' modifier" "true"

class A() {
    @Deprecated("") // wd
    final<caret> constructor(i: Int): this()
}
/* IGNORE_FIR */