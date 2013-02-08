class Derived(): simpleProperty() {
    fun test(): String {
        return simpleProperty.protectedProperty!!
    }
}

fun box(): String {
   return Derived().test()
}

