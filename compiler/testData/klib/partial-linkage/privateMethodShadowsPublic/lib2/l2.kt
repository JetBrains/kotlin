class ClassWithPrivateShadowingFinal : ClassWithAddedFinal() {
   private fun foo() = 3
}

class ClassWithPrivateShadowingOpen : ClassWithAddedOpen() {
   private fun foo() = 3
}

class ClassWithPrivateShadowingAbstract : ClassWithAddedAbstract() {
   private fun foo() = 3
}


class ClassWithInternalShadowingFinal : ClassWithAddedFinal() {
   internal fun foo() = 3
}

class ClassWithInternalShadowingOpen : ClassWithAddedOpen() {
   internal fun foo() = 3
}

class ClassWithInternalShadowingAbstract : ClassWithAddedAbstract() {
   internal fun foo() = 3
}
