@Target(AnnotationTarget.CONSTRUCTOR)
annotation class Ctor

class WithAnnotatedConstructor @Ctor constructor(val x: Int)

class WithAnnotatedPrivateConstructor @Ctor private constructor()

// The `constructor` keyword is rendered even without a parameter list.
class NoParameters @Ctor constructor()
