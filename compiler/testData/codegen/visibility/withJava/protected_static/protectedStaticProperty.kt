class Derived(): protectedStaticProperty() {
    fun test(): String {
        return protectedStaticProperty.protectedProperty!!
    }
}

fun box(): String {
   return Derived().test()
}

