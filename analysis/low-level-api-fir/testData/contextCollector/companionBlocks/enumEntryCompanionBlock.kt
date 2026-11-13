// LANGUAGE: +CompanionBlocks +CompanionExtensions

enum class E {
    A {
        companion {
            fun foo(): Int = <expr>1</expr>
        }
    };
}
