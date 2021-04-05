package foo

actual sealed class <!LINE_MARKER("descr='Is subclassed by Derived12 Derived13Error'"), LINE_MARKER("descr='Has declaration in common module'")!>Sealed2<!> : Sealed1()

class Derived2 : Sealed1()
class Derived12 : Sealed2()
