package test

interface KotlinInterface

class Impl1 : KotlinInterface

class Impl2 : KotlinInterface

class Impl3 : KotlinInterface

fun getProducer1() = Impl1().let(::ClassWithTypeParameter)

fun getProducer2() = Impl2().let(::ClassWithTypeParameter)

fun getProducer3() = Impl3().let(::ClassWithTypeParameter)
