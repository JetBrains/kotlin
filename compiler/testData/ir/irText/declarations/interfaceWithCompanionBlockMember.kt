// LANGUAGE: +CompanionBlocksAndExtensions
// ISSUE: KT-86955

interface MyInterface {
    companion {
        fun interfaceFun() = "InterfaceFun"
    }
}
