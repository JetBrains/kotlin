// WITH_FIR_TEST_COMPILER_PLUGIN
package test

import org.jetbrains.kotlin.plugin.sandbox.AllOpen

typealias MyAllOpen = AllOpen

@MyAllOpen
class Aliased

@Deprecated("")
@MyAllOpen
class AliasedWithDeprecated

@AllOpen
class Direct
