// FIR_IDENTICAL
// SKIP_TXT
fun MutableList<out CharSequence>.foo() {
    this.add(this.get(0))
}
