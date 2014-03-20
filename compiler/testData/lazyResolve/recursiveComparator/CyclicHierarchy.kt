package test

trait A
trait B : A, E
trait C : B
trait D : B
trait E : F
trait F : D, C
trait G : F
trait H : F