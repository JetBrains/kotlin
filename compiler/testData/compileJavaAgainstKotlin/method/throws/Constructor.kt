package test

class E1: Exception()
class E2: Exception()

class None @Throws() constructor() {}
class One @Throws(E1::class) constructor()
class Two @Throws(E1::class, E2::class) constructor()

class OneWithParam @Throws(E1::class) constructor(a: Int)