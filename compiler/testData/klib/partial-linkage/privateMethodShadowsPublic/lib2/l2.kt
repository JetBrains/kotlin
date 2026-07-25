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

class ClassWithInternalPAShadowingFinal : ClassWithAddedFinal() {
   @PublishedApi internal fun foo() = 3
}
class ClassWithInternalPAShadowingOpen : ClassWithAddedOpen() {
   @PublishedApi internal fun foo() = 3
}
class ClassWithInternalPAShadowingAbstract : ClassWithAddedAbstract() {
   @PublishedApi internal fun foo() = 3
}

class ClassWithProtectedShadowingFinal : ClassWithAddedFinal() {
   protected fun foo() = 3
}
class ClassWithProtectedShadowingOpen : ClassWithAddedOpen() {
   protected fun foo() = 3
}
class ClassWithProtectedShadowingAbstract : ClassWithAddedAbstract() {
   protected fun foo() = 3
}
