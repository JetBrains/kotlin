package test

class Inheritor3 : SealedInterface

sealed interface SealedInterface {
    class Inheritor1 : SealedInterface
}

class Inheritor2 : SealedInterface
