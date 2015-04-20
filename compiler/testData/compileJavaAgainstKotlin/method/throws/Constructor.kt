package test

class E1: Exception()
class E2: Exception()

class None [throws()]() {}
class One [throws(E1::class)]()
class Two [throws(E1::class, E2::class)]()

class OneWithParam [throws(E1::class)](a: Int)