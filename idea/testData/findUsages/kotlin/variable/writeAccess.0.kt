// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetProperty
// OPTIONS: usages
// OPTIONS: skipRead
fun foo() {
    var <caret>v = 1
    v = 2
    print(v)
    ++v
    v--
    print(-v)
    v += 1
    v -= 1
}
