// LIBRARY_PLATFORMS: JVM

import kotlin.jvm.JvmStatic as JS

object O {
  @JS fun foo() {}
}

// LIGHT_ELEMENTS_NO_DECLARATION: O.class[O]