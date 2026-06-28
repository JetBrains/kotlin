// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
sealed interface ShellName {
  /** The lowercase name of the shell */
  val value: String

  companion object {
    fun of(value: String): ShellName = ShellNameImpl(value.lowercase())

    val BASH: ShellName = of("bash")
    val ZSH: ShellName = of("zsh")
    val FISH: ShellName = of("fish")
    val POWERSHELL: ShellName = of("powershell")
    val PWSH: ShellName = of("pwsh")

    fun isPowerShell(shellName: ShellName): Boolean {
      return shellName == POWERSHELL || shellName == PWSH
    }
  }
}

private data class ShellNameImpl(override val value: String) : ShellName

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, disjunctionExpression, equalityExpression,
functionDeclaration, interfaceDeclaration, objectDeclaration, override, primaryConstructor, propertyDeclaration, sealed,
stringLiteral */
