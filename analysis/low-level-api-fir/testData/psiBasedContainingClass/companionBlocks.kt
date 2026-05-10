// LANGUAGE: +CompanionBlocksAndExtensions
// ALLOW_PSI_PRESENCE: <local>/field
// ^KT-85884

class Foo {
    companion {
        fun staticFunction() {}
        val staticProperty: Int = 42
    }
}
