package test

interface KotlinInterface

object Impl : KotlinInterface

fun useMethod() = MethodWithTypeParameter.method(Impl)
