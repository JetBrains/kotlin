package test

class E1: Exception()
class E2: Exception()

class None [throws()]() {}
class One [throws(javaClass<E1>())]()
class Two [throws(javaClass<E1>(), javaClass<E2>())]()

class OneWithParam [throws(javaClass<E1>())](a: Int)