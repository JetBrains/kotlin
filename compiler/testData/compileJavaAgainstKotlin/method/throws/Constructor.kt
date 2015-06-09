package test

class E1: Exception()
class E2: Exception()

class None @throws() constructor() {}
class One @throws(E1::class) constructor()
class Two @throws(E1::class, E2::class) constructor()

class OneWithParam @throws(E1::class) constructor(a: Int)